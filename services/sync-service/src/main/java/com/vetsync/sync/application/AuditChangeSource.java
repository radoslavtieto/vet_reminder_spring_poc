package com.vetsync.sync.application;

import com.vetsync.sync.domain.AuditChange;

import java.time.Instant;
import java.util.List;

public interface AuditChangeSource {
    List<AuditChange> readMasterData(
            String tenant,
            long lastAuditId,
            Instant overlapFrom,
            Instant safeBefore,
            int batchSize
    );
}
