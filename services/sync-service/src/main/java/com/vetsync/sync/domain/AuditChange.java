package com.vetsync.sync.domain;

import com.vetsync.contracts.sync.SyncOperation;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditChange(
        long auditId,
        String entityType,
        UUID publicId,
        SyncOperation operation,
        long sourceVersion,
        Instant changedAt,
        Map<String, Object> payload) {

    public AuditChange {
        if (auditId <= 0) {
            throw new IllegalArgumentException("auditId must be positive");
        }
        entityType = requireText(entityType, "entityType");
        Objects.requireNonNull(publicId, "publicId");
        Objects.requireNonNull(operation, "operation");
        if (sourceVersion < 0) {
            throw new IllegalArgumentException("sourceVersion must not be negative");
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
