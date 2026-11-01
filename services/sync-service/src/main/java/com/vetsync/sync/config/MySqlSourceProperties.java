package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sync.source.mysql")
public record MySqlSourceProperties(String url, String username, String password) {

    public MySqlSourceProperties {
        url = requireText(url, "SYNC_MYSQL_URL");
        username = requireText(username, "SYNC_MYSQL_USERNAME");
        password = requireText(password, "SYNC_MYSQL_PASSWORD");
    }

    private static String requireText(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be configured when the MySQL source is enabled");
        }
        return value;
    }
}
