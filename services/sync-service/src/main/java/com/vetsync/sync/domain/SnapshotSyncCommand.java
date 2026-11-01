package com.vetsync.sync.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SnapshotSyncCommand(
        TenantCode tenant,
        SnapshotEntity entity,
        SnapshotMode mode,
        String runId,
        UUID publicId,
        Instant updatedFromInclusive,
        Instant updatedToExclusive,
        int batchSize,
        String leaseOwner,
        Duration leaseDuration) {

    public SnapshotSyncCommand {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(mode, "mode");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (leaseOwner == null || leaseOwner.isBlank()) {
            throw new IllegalArgumentException("leaseOwner must not be blank");
        }
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        if (mode == SnapshotMode.INITIAL) {
            if (!"initial".equals(runId) || publicId != null
                    || updatedFromInclusive != null || updatedToExclusive != null) {
                throw new IllegalArgumentException("Initial snapshot must use runId 'initial' without filters");
            }
        } else {
            boolean hasAnyTimeBound = updatedFromInclusive != null || updatedToExclusive != null;
            if (hasAnyTimeBound && (updatedFromInclusive == null || updatedToExclusive == null)) {
                throw new IllegalArgumentException("Both partial snapshot time bounds must be provided");
            }
            if (hasAnyTimeBound && !updatedFromInclusive.isBefore(updatedToExclusive)) {
                throw new IllegalArgumentException("Partial snapshot start must be before its end");
            }
            if (publicId != null && hasAnyTimeBound) {
                throw new IllegalArgumentException("Partial snapshot accepts either publicId or a time range");
            }
        }
    }

    public String progressScope() {
        return mode == SnapshotMode.INITIAL ? "initial" : "partial:" + runId;
    }
}
