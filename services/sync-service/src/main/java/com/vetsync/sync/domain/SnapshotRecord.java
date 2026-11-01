package com.vetsync.sync.domain;

import com.vetsync.contracts.sync.SyncOperation;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SnapshotRecord(
        long sourceId,
        UUID publicId,
        String entityType,
        long sourceVersion,
        Instant occurredAt,
        SyncOperation operation,
        Map<String, Object> payload) {

    public SnapshotRecord {
        if (sourceId <= 0) {
            throw new IllegalArgumentException("sourceId must be positive");
        }
        Objects.requireNonNull(publicId, "publicId");
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(operation, "operation");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload")));
    }
}
