package com.vetsync.pms;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "VetSync PMS API",
        version = "v1",
        description = "Region-scoped practice management API for clinics, clients, patients, visits, "
                + "prescriptions, and invoices."))
public class PmsApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(PmsApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PmsApiApplication.class, args);
        logger.info("Service started");
    }
}
