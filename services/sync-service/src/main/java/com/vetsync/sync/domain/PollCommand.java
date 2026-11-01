package com.vetsync.sync.domain;

import java.time.Duration;
import java.util.Objects;

public record PollCommand(
        TenantCode tenant,
        SyncStream stream,
        int batchSize,
        Duration safetyLag,
        int overlapAuditRows,
        String leaseOwner,
        Duration leaseDuration) {

    public PollCommand {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(safetyLag, "safetyLag");
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (safetyLag.isNegative()) {
            throw new IllegalArgumentException("safetyLag must not be negative");
        }
        if (overlapAuditRows < 0) {
            throw new IllegalArgumentException("overlapAuditRows must not be negative");
        }
        if (overlapAuditRows >= batchSize) {
            throw new IllegalArgumentException("overlapAuditRows must be smaller than batchSize");
        }
        if (leaseOwner == null || leaseOwner.isBlank()) {
            throw new IllegalArgumentException("leaseOwner must not be blank");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
    }
}
