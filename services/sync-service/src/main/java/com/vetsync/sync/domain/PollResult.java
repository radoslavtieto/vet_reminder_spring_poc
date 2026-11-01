package com.vetsync.sync.domain;

public record PollResult(
        PollStatus status,
        int readCount,
        int publishedCount,
        int duplicateCount,
        long lastAuditId) {
}
