package com.vetsync.sync.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record TimeWindowRecord(
        long sourceId,
        String entityType,
        UUID publicId,
        long sourceVersion,
        Instant occurredAt,
        Map<String, Object> payload) {

    public TimeWindowRecord {
        if (sourceId < 1) {
            throw new IllegalArgumentException("sourceId must be positive");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        Objects.requireNonNull(publicId, "publicId");
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload")));
    }
}
