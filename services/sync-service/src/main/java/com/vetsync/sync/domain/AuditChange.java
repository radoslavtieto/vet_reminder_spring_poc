package com.vetsync.sync.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditChange(
        long auditId,
        String tenant,
        String entityType,
        UUID publicId,
        String operation,
        long rowVersion,
        Instant changedAt,
        JsonNode payload
) {
    public AuditChange {
        if (auditId <= 0 || rowVersion < 0) {
            throw new IllegalArgumentException("Audit id must be positive and row version non-negative");
        }
        if (tenant == null || tenant.isBlank() || entityType == null || entityType.isBlank()
                || operation == null || operation.isBlank() || publicId == null || changedAt == null
                || payload == null) {
            throw new IllegalArgumentException("Audit change fields must not be null or blank");
        }
    }
}
