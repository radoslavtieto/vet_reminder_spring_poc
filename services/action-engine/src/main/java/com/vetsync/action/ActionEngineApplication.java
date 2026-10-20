package com.vetsync.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ActionEngineApplication {

    private static final Logger logger = LoggerFactory.getLogger(ActionEngineApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ActionEngineApplication.class, args);
        logger.info("Service started");
    }
}
