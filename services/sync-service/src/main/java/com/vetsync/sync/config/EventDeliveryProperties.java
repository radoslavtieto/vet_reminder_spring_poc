package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "sync.messaging.delivery")
public record EventDeliveryProperties(int maxAttempts, Duration retryDelay) {

    public EventDeliveryProperties {
        Objects.requireNonNull(retryDelay, "retryDelay");
        if (maxAttempts < 1) {
            throw new IllegalStateException("SYNC_EVENT_MAX_ATTEMPTS must be positive");
        }
        if (retryDelay.isNegative()) {
            throw new IllegalStateException("SYNC_EVENT_RETRY_DELAY must not be negative");
        }
    }
}
