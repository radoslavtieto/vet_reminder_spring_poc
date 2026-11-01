package com.vetsync.sync.domain;

import java.util.Objects;

public record SnapshotProgress(
        TenantCode tenant,
        SnapshotEntity entity,
        String runId,
        long lastId,
        SnapshotProgressState state) {

    public SnapshotProgress {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(entity, "entity");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (lastId < 0) {
            throw new IllegalArgumentException("lastId must not be negative");
        }
        Objects.requireNonNull(state, "state");
    }
}
