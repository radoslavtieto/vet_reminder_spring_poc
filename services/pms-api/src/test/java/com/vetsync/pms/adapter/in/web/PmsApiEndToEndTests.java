package com.vetsync.pms.adapter.in.web;

import com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicRequest;
import com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
@Tag("e2e")
class PmsApiEndToEndTests {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("vetsync_e2e")
            .withUsername("vetsync_test")
            .withPassword("synthetic_test_password")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Test
    @DisplayName("Given a new clinic, when it is created over HTTP, then it can be fetched only in its tenant")
    void givenNewClinicWhenCreatedOverHttpThenItCanBeFetchedOnlyInItsTenant() {
        ClinicRequest request = new ClinicRequest("Synthetic E2E Clinic", "Sample City",
                null, null, null, true);

        ResponseEntity<ClinicView> created = http.postForEntity(url("/api/v1/sw/clinics"),
                request, ClinicView.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().tenant()).isEqualTo("SW");

        ResponseEntity<ClinicView> fetched = http.getForEntity(created.getHeaders().getLocation(),
                ClinicView.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().name()).isEqualTo("Synthetic E2E Clinic");

        ResponseEntity<String> crossTenant = http.getForEntity(
                url("/api/v1/ne/clinics/" + created.getBody().id()), String.class);
        assertThat(crossTenant.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
