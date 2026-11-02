package com.vetsync.action.application;

import com.vetsync.contracts.SyncEnvelope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncEventProcessor {
    private final ActionDataStore store;
    private final SyncEnvelopeMapper mapper;

    public SyncEventProcessor(ActionDataStore store, SyncEnvelopeMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    @Transactional
    public void process(SyncEnvelope event) {
        if (!store.claimEvent(event.eventId(), event.tenant())) {
            return;
        }
        store.upsert(mapper.map(event));
    }
}
