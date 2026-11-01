package com.vetsync.sync.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SyncRun(
        UUID id,
        TenantCode tenant,
        String scope,
        SyncRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorType) {

    public SyncRun {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenant, "tenant");
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
        if (status == SyncRunStatus.RUNNING) {
            if (finishedAt != null || errorType != null) {
                throw new IllegalArgumentException("Running sync run cannot have finish time or error type");
            }
        } else {
            Objects.requireNonNull(finishedAt, "finishedAt");
            if (status == SyncRunStatus.COMPLETED && errorType != null) {
                throw new IllegalArgumentException("Completed sync run cannot have an error type");
            }
            if (status == SyncRunStatus.FAILED && (errorType == null || errorType.isBlank())) {
                throw new IllegalArgumentException("Failed sync run must include an error type");
            }
        }
    }
}
