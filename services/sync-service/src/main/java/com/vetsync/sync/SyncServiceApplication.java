package com.vetsync.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
public class SyncServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(SyncServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
        logger.info("Service started");
    }
}
