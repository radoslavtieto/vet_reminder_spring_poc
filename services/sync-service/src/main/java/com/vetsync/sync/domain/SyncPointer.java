package com.vetsync.sync.domain;

import java.util.Objects;

public record SyncPointer(TenantCode tenant, SyncStream stream, long lastAuditId) {

    public SyncPointer {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(stream, "stream");
        if (lastAuditId < 0) {
            throw new IllegalArgumentException("lastAuditId must not be negative");
        }
    }
}
