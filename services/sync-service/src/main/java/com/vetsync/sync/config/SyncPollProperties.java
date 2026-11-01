package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "sync.poll")
public record SyncPollProperties(
        boolean enabled,
        int batchSize,
        Duration safetyLag,
        int overlapAuditRows,
        Duration leaseDuration,
        long auditFixedDelayMs) {

    public SyncPollProperties {
        Objects.requireNonNull(safetyLag, "safetyLag");
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (batchSize < 1) {
            throw new IllegalStateException("SYNC_POLL_BATCH_SIZE must be positive");
        }
        if (safetyLag.isNegative()) {
            throw new IllegalStateException("SYNC_POLL_SAFETY_LAG must not be negative");
        }
        if (overlapAuditRows < 0 || overlapAuditRows >= batchSize) {
            throw new IllegalStateException("SYNC_POLL_OVERLAP_AUDIT_ROWS must be non-negative and smaller than batch size");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalStateException("SYNC_POLL_LEASE_DURATION must be positive");
        }
        if (auditFixedDelayMs < 1) {
            throw new IllegalStateException("SYNC_AUDIT_POLL_DELAY_MS must be positive");
        }
    }
}
