package com.vetsync.sync.domain;

import com.vetsync.contracts.sync.SyncOperation;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditChange(
        long auditId,
        String tenant,
        String entityType,
        UUID publicId,
        String operation,
        long rowVersion,
        SyncOperation operation,
        long sourceVersion,
        Instant changedAt,
        JsonNode payload
) {
        Map<String, Object> payload) {

    public AuditChange {
        if (auditId <= 0 || rowVersion < 0) {
            throw new IllegalArgumentException("Audit id must be positive and row version non-negative");
        if (auditId <= 0) {
            throw new IllegalArgumentException("auditId must be positive");
        }
        entityType = requireText(entityType, "entityType");
        Objects.requireNonNull(publicId, "publicId");
        Objects.requireNonNull(operation, "operation");
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
        }
        if (tenant == null || tenant.isBlank() || entityType == null || entityType.isBlank()
                || operation == null || operation.isBlank() || publicId == null || changedAt == null
                || payload == null) {
            throw new IllegalArgumentException("Audit change fields must not be null or blank");
        }
        Objects.requireNonNull(changedAt, "changedAt");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload")));
    }

    public String idempotencyKey(TenantCode tenant) {
        return tenant.name() + ":" + entityType + ":" + publicId + ":" + sourceVersion;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
