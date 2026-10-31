package com.vetsync.contracts;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
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
        JsonNode payload
) {
    private static final Set<String> TENANTS = Set.of("SW", "SE", "SC", "NE", "NW", "NC");

    public SyncEnvelope {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported SyncEnvelope schema version: " + schemaVersion);
        }
        require(eventId, "eventId");
        require(correlationId, "correlationId");
        if (tenant == null || !TENANTS.contains(tenant)) {
            throw new IllegalArgumentException("Unsupported tenant: " + tenant);
        }
        requireText(stream, "stream");
        requireText(entityType, "entityType");
        require(entityId, "entityId");
        require(operation, "operation");
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
        require(occurredAt, "occurredAt");
        require(publishedAt, "publishedAt");
        require(payload, "payload");
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
