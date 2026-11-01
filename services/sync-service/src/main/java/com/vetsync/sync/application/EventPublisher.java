package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;

public interface EventPublisher {

    void publish(SyncEnvelope envelope);
}
