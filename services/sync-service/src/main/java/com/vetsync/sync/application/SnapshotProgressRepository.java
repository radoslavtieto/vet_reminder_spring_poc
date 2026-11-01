package com.vetsync.sync.application;

import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotProgress;
import com.vetsync.sync.domain.TenantCode;

import java.util.Optional;

public interface SnapshotProgressRepository {

    Optional<SnapshotProgress> find(TenantCode tenant, SnapshotEntity entity, String runId);

    void save(SnapshotProgress progress);
}
