package com.vetsync.sync.domain;

public record SnapshotSyncResult(
        SnapshotSyncStatus status,
        int readCount,
        int publishedCount,
        int duplicateCount,
        long lastId) {
}
