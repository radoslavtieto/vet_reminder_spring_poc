package com.vetsync.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(SyncServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
        logger.info("Service started");
    }
}
