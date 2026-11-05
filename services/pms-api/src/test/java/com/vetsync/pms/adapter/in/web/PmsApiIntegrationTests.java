package com.vetsync.pms.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class PmsApiIntegrationTests {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("vetsync_api")
            .withUsername("vetsync_test")
            .withPassword("synthetic_test_password")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void clinicCrudAndNestedStaffAreRegionScopedAndRejectInvalidOrForeignParents() throws Exception {
        String southWestAdmin = bearer("SW");
        String northEastAdmin = bearer("NE");
        String clinicPayload = """
                {"name":"Synthetic South-West Clinic","city":"Sample City","active":true}
                """;
        String clinicResponse = mockMvc.perform(post("/api/v1/sw/clinics")
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clinicPayload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Synthetic South-West Clinic"))
                .andReturn().getResponse().getContentAsString();
        JsonNode clinic = objectMapper.readTree(clinicResponse);
        long clinicId = clinic.get("id").asLong();

        mockMvc.perform(get("/api/v1/sw/clinics/{id}", clinicId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value("SW"));
        mockMvc.perform(get("/api/v1/ne/clinics/{id}", clinicId)
                        .header("Authorization", northEastAdmin))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/sw/clinics/{id}/staff", clinicId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VET","fullName":"Synthetic Vet","licenseNo":"LIC-TEST-1","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
        mockMvc.perform(get("/api/v1/sw/clinics/{id}/staff", clinicId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Synthetic Vet"));
        mockMvc.perform(get("/api/v1/ne/clinics/{id}/staff", clinicId)
                        .header("Authorization", northEastAdmin))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/sw/clinics/{id}/staff", clinicId + 999)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VET","fullName":"Orphan Staff","active":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/sw/clinics")
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","active":true}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/sw/clinics/{id}", clinicId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Synthetic Clinic","city":"Sample City","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Synthetic Clinic"))
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/sw/clinics/{id}", clinicId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/sw/clinics/{id}", clinicId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isNotFound());
        assertThat(clinic.get("tenant").asText()).isEqualTo("SW");
    }

    @Test
    void nestedPatientCreationBindsClientAndRejectsCrossTenantAndInvalidAssociations() throws Exception {
        String southWestAdmin = bearer("SW");
        String northEastAdmin = bearer("NE");
        long clinicId = createClinic("sw", southWestAdmin);
        String clientBody = """
                {"registeredClinicId":%d,"firstName":"Sample","lastName":"Owner","phone":"555-0101"}
                """.formatted(clinicId);
        String clientJson = mockMvc.perform(post("/api/v1/sw/clients")
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(clientBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long clientId = objectMapper.readTree(clientJson).get("id").asLong();

        String speciesCode = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        jdbcTemplate.update("""
                INSERT INTO species (tenant_id, public_id, code, name) VALUES ('SW', ?, ?, ?)
                """, UUID.randomUUID().toString(), speciesCode, "Synthetic species");
        long speciesId = jdbcTemplate.queryForObject(
                "SELECT id FROM species WHERE tenant_id = 'SW' AND code = ?", Long.class, speciesCode);

        String patientJson = mockMvc.perform(post("/api/v1/sw/clients/{id}/patients", clientId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clinicId":%d,"name":"Synthetic Patient","speciesId":%d,"neutered":false}
                                """.formatted(clinicId, speciesId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(Math.toIntExact(clientId)))
                .andReturn().getResponse().getContentAsString();
        long patientId = objectMapper.readTree(patientJson).get("id").asLong();

        mockMvc.perform(get("/api/v1/sw/clients/{id}/patients", clientId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Math.toIntExact(patientId)));
        mockMvc.perform(get("/api/v1/ne/patients/{id}", patientId)
                        .header("Authorization", northEastAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/sw/clients/{id}/patients", clientId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"clinicId":%d,"name":"Wrong Parent","speciesId":%d,"neutered":false}
                                """.formatted(clientId + 1, clinicId, speciesId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/sw/clients?q=555-0101")
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Math.toIntExact(clientId)));

        String staffJson = mockMvc.perform(post("/api/v1/sw/clinics/{id}/staff", clinicId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VET","fullName":"Synthetic Vet","licenseNo":"LIC-%s","active":true}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long staffId = objectMapper.readTree(staffJson).get("id").asLong();

        String visitJson = mockMvc.perform(post("/api/v1/sw/patients/{id}/visits", patientId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clinicId":%d,"vetId":%d,"visitType":"CHECKUP",
                                 "scheduledAt":"2026-10-01T10:00:00Z"}
                                """.formatted(clinicId, staffId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long visitId = objectMapper.readTree(visitJson).get("id").asLong();

        mockMvc.perform(post("/api/v1/sw/visits/{id}/prescriptions", visitId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"vetId":%d,"issuedOn":"2026-09-30",
                                 "validUntil":"2026-10-14","notes":"Synthetic test"}
                                """.formatted(patientId, staffId)))
                .andExpect(status().isCreated());

        String invoiceJson = mockMvc.perform(post("/api/v1/sw/invoices")
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clinicId":%d,"clientId":%d,"visitId":%d,"invoiceNo":"INV-%s",
                                 "status":"ISSUED","issuedOn":"2026-09-30","dueOn":"2026-10-14",
                                 "currency":"EUR","totalAmount":25.00,"paidAmount":0.00}
                                """.formatted(clinicId, clientId, visitId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long invoiceId = objectMapper.readTree(invoiceJson).get("id").asLong();
        String paymentJson = mockMvc.perform(post("/api/v1/sw/invoices/{id}/payments", invoiceId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paidAt":"2026-09-30T10:00:00Z","amount":10.00,"method":"CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paymentId = objectMapper.readTree(paymentJson).get("id").asLong();
        mockMvc.perform(get("/api/v1/sw/invoices/{id}/payments/{paymentId}", invoiceId, paymentId)
                        .header("Authorization", southWestAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(Math.toIntExact(invoiceId)));
        mockMvc.perform(post("/api/v1/sw/invoices/{id}/payments", invoiceId)
                        .header("Authorization", southWestAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paidAt":"2026-09-30T10:00:00Z","amount":20.00,"method":"CARD"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private long createClinic(String region, String authorization) throws Exception {
        String response = mockMvc.perform(post("/api/v1/{region}/clinics", region)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Synthetic Clinic %s","active":true}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String bearer(String tenant) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vetsync-pms-api")
                .subject("test-user")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .claim("tenant", tenant)
                .claim("role", "ADMIN")
                .build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
