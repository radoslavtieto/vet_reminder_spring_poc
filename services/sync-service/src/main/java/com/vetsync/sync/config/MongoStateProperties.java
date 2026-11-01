package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sync.state.mongo")
public record MongoStateProperties(String uri, String database, int dedupeRetentionDays) {

    public MongoStateProperties {
        uri = requireText(uri, "SYNC_MONGODB_URI");
        database = requireText(database, "SYNC_MONGODB_DATABASE");
        if (dedupeRetentionDays < 1) {
            throw new IllegalStateException("SYNC_DEDUPE_RETENTION_DAYS must be positive");
        }
    }

    private static String requireText(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be configured when Mongo state is enabled");
        }
        return value;
    }
}
