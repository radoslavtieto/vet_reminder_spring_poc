package com.vetsync.pms.adapter.in.web;

import com.vetsync.pms.adapter.in.web.dto.AuthDto.CreateUserRequest;
import com.vetsync.pms.adapter.in.web.dto.AuthDto.TokenResponse;
import com.vetsync.pms.adapter.in.web.dto.AuthDto.UserView;
import com.vetsync.pms.adapter.in.web.controller.AuthController;
import com.vetsync.pms.adapter.in.web.controller.ClientController;
import com.vetsync.pms.adapter.in.web.controller.InvoiceController;
import com.vetsync.pms.adapter.in.web.controller.PatientController;
import com.vetsync.pms.application.PmsApplicationService;
import com.vetsync.pms.application.PmsAuthenticationService;
import com.vetsync.pms.config.PmsSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, ClientController.class,
        InvoiceController.class, PatientController.class},
        properties = "pms.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Import(PmsSecurityConfiguration.class)
class PmsSecurityWebTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockitoBean private PmsApplicationService application;
    @MockitoBean private PmsAuthenticationService authentication;

    @Test
    void loginIsPublicAndDelegatesToTenantAuthentication() throws Exception {
        when(application.tenant("sw")).thenReturn("SW");
        when(authentication.login("SW", "admin", "strong-password"))
                .thenReturn(new TokenResponse("token-value", "Bearer", 900, "SW", "ADMIN"));

        mockMvc.perform(post("/api/v1/sw/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"strong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.tenant").value("SW"));
    }

    @Test
    void userManagementRequiresAdminRoleAndRejectsMissingCredentials() throws Exception {
        String payload = """
                {"username":"staff@example.invalid","password":"strong-password","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/v1/sw/users").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/sw/users").header("Authorization", bearer("SW", "RECEPTION"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());

        when(application.tenant("sw")).thenReturn("SW");
        when(authentication.createUser(any(), any(CreateUserRequest.class)))
                .thenReturn(new UserView(2L, "staff@example.invalid", "ADMIN", null, true));
        mockMvc.perform(post("/api/v1/sw/users").header("Authorization", bearer("SW", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void validAdminTokenCannotAccessAnotherTenant() throws Exception {
        mockMvc.perform(post("/api/v1/se/users").header("Authorization", bearer("SW", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff@example.invalid","password":"strong-password","role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionCanUseClientAndInvoiceRoutesButCannotReadClinicalRecords() throws Exception {
        when(application.tenant("sw")).thenReturn("SW");
        when(application.clients("SW", null)).thenReturn(List.of());
        when(application.invoices("SW", null)).thenReturn(List.of());
        String reception = bearer("SW", "RECEPTION");

        mockMvc.perform(get("/api/v1/sw/clients").header("Authorization", reception))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/sw/invoices").header("Authorization", reception))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/sw/patients").header("Authorization", reception))
                .andExpect(status().isForbidden());
    }

    private String bearer(String tenant, String role) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vetsync-pms-api")
                .subject("test-user")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .claim("tenant", tenant)
                .claim("role", role)
                .build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
