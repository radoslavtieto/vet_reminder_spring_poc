package com.vetsync.pms.adapter.in.web;

import com.vetsync.pms.adapter.in.web.controller.ClinicController;
import com.vetsync.pms.adapter.in.web.controller.ClientController;
import com.vetsync.pms.adapter.in.web.controller.InvoiceController;
import com.vetsync.pms.adapter.in.web.controller.PatientController;
import com.vetsync.pms.adapter.in.web.controller.PrescriptionController;
import com.vetsync.pms.adapter.in.web.controller.StaffController;
import com.vetsync.pms.adapter.in.web.controller.VisitController;
import com.vetsync.pms.application.PmsApplicationService;
import com.vetsync.pms.application.PmsResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ClinicController.class,
        StaffController.class,
        ClientController.class,
        PatientController.class,
        VisitController.class,
        PrescriptionController.class,
        InvoiceController.class
})
@Import(PmsApiValidationTests.PermitAllTestSecurityConfiguration.class)
class PmsApiValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @MockitoBean
    private PmsApplicationService service;

    @TestConfiguration
    static class PermitAllTestSecurityConfiguration {
        @Bean
        SecurityFilterChain permitAllTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void configuresStructuredLogsAndServiceIdentity() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("pms-api");
        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("logstash");
    }

    @Test
    void invalidClinicPayloadReturnsBadRequestBeforeCallingApplicationService() throws Exception {
        mockMvc.perform(post("/api/v1/sw/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","active":true}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void validationFailureDoesNotExposeOrLogRejectedFieldValues(CapturedOutput output) throws Exception {
        String rejectedValue = "Synthetic Sensitive Clinic Name " + "x".repeat(150);

        mockMvc.perform(post("/api/v1/sw/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","active":true}
                                """.formatted(rejectedValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));

        assertThat(output.getAll()).doesNotContain(rejectedValue);
        verifyNoInteractions(service);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void databaseConflictIsLoggedWithoutExposingDatabaseDetails(CapturedOutput output) throws Exception {
        String databaseDetail = "private database constraint detail";
        when(service.tenant("sw")).thenReturn("SW");
        when(service.createClinic(anyString(), any()))
                .thenThrow(new DataIntegrityViolationException(databaseDetail));

        mockMvc.perform(post("/api/v1/sw/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Synthetic Clinic","active":true}
                                """))
                .andExpect(status().isConflict());

        assertThat(output.getAll())
                .contains("Request rejected because it conflicts with existing PMS data")
                .doesNotContain(databaseDetail);
    }

    @Test
    void applicationNotFoundIsRenderedAsHttp404() throws Exception {
        when(service.tenant("xx")).thenThrow(new PmsResourceNotFoundException("Unknown region"));

        mockMvc.perform(get("/api/v1/xx/clinics"))
                .andExpect(status().isNotFound());
    }
}
