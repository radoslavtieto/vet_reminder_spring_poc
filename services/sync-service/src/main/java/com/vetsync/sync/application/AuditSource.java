package com.vetsync.sync.application;

import com.vetsync.sync.domain.AuditChange;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;

import java.time.Instant;
import java.util.List;

public interface AuditSource {

    List<AuditChange> findChanges(
            TenantCode tenant,
            SyncStream stream,
            long fromAuditIdInclusive,
            Instant changedBefore,
            int limit);
}
