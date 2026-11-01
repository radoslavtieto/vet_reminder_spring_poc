package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sync.scan")
public record TimeWindowScanProperties(
        int expiryLeadDays,
        int invoiceLookbackMonths,
        int invoiceGraceDays,
        long fixedDelayMs) {

    public TimeWindowScanProperties {
        if (expiryLeadDays < 0) {
            throw new IllegalStateException("SYNC_EXPIRY_LEAD_DAYS must not be negative");
        }
        if (invoiceLookbackMonths < 1) {
            throw new IllegalStateException("SYNC_UNPAID_INVOICE_LOOKBACK_MONTHS must be positive");
        }
        if (invoiceGraceDays < 0) {
            throw new IllegalStateException("SYNC_INVOICE_GRACE_DAYS must not be negative");
        }
        if (fixedDelayMs < 1) {
            throw new IllegalStateException("SYNC_TIME_WINDOW_POLL_DELAY_MS must be positive");
        }
    }
}
