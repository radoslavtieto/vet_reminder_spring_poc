package com.vetsync.sync.application;

import com.vetsync.sync.domain.SyncPointer;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;

import java.util.Optional;

public interface PointerRepository {

    Optional<SyncPointer> find(TenantCode tenant, SyncStream stream);

    void save(SyncPointer pointer);
}
