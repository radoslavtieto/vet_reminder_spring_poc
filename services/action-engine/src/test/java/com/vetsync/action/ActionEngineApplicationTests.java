package com.vetsync.action;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ActionEngineApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void configuresStructuredLogsAndServiceIdentity() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("action-engine");
        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("logstash");
    }
}
