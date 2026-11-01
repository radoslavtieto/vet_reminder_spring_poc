package com.vetsync.sync.domain;

import java.time.Duration;
import java.util.Objects;

public record TimeWindowScanCommand(
        TenantCode tenant,
        SyncStream stream,
        int batchSize,
        int expiryLeadDays,
        int invoiceLookbackMonths,
        int invoiceGraceDays,
        String leaseOwner,
        Duration leaseDuration) {

    public TimeWindowScanCommand {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (stream.isAuditStream()) {
            throw new IllegalArgumentException("Stream is not a time-window scan: " + stream.contractName());
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (expiryLeadDays < 0) {
            throw new IllegalArgumentException("expiryLeadDays must not be negative");
        }
        if (invoiceLookbackMonths < 1) {
            throw new IllegalArgumentException("invoiceLookbackMonths must be positive");
        }
        if (invoiceGraceDays < 0) {
            throw new IllegalArgumentException("invoiceGraceDays must not be negative");
        }
        if (leaseOwner == null || leaseOwner.isBlank()) {
            throw new IllegalArgumentException("leaseOwner must not be blank");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
    }
}
