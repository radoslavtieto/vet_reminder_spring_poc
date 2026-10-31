package com.vetsync.sync.application;

import com.vetsync.contracts.SyncEnvelope;

public interface SyncEventPublisher {
    void publish(SyncEnvelope event);
}
