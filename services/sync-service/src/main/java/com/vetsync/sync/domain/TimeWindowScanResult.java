package com.vetsync.sync.domain;

public record TimeWindowScanResult(
        TimeWindowScanStatus status,
        int readCount,
        int publishedCount,
        int duplicateCount) {
}
