package com.vetsync.contracts.sync;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SyncEnvelope(
        int schemaVersion,
        UUID eventId,
        UUID correlationId,
        String tenant,
        String stream,
        String entityType,
        UUID entityId,
        SyncOperation operation,
        long sourceVersion,
        Instant occurredAt,
        Instant publishedAt,
        Map<String, Object> payload) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public SyncEnvelope {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported sync envelope schema version: " + schemaVersion);
        }
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(correlationId, "correlationId");
        tenant = requireText(tenant, "tenant");
        if (!Set.of("SW", "SE", "SC", "NE", "NW", "NC").contains(tenant)) {
            throw new IllegalArgumentException("Unsupported tenant: " + tenant);
        }
        stream = requireText(stream, "stream");
        if (!Set.of(
                "master-data", "visits", "prescriptions", "invoices", "prescription-expiry", "unpaid-invoices")
                .contains(stream)) {
            throw new IllegalArgumentException("Unsupported stream: " + stream);
        }
        entityType = requireText(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(operation, "operation");
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(publishedAt, "publishedAt");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload")));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
