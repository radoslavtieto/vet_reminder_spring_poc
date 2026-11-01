package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;

public interface EventQuarantine {

    void quarantine(SyncEnvelope envelope, String failureReason);
}
