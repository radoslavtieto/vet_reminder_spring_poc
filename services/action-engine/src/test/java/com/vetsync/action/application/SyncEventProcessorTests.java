package com.vetsync.action.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.SyncEnvelope;
import com.vetsync.contracts.SyncOperation;
import com.vetsync.action.domain.Contact;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class SyncEventProcessorTests {

    private final ActionDataStore store = mock(ActionDataStore.class);
    private final SyncEventProcessor processor = new SyncEventProcessor(store, new SyncEnvelopeMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesTheMappedRecordOnlyWhenTheInboxClaimsANewEvent() {
        SyncEnvelope event = clientEvent();
        when(store.claimEvent(event.eventId(), event.tenant())).thenReturn(true);

        processor.process(event);

        verify(store).upsert(any(Contact.class));
    }

    @Test
    void ignoresRedeliveredEventsAlreadyInTheInbox() {
        SyncEnvelope event = clientEvent();
        when(store.claimEvent(event.eventId(), event.tenant())).thenReturn(false);

        processor.process(event);

        verify(store, never()).upsert(any(Contact.class));
    }

    private SyncEnvelope clientEvent() {
        return new SyncEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), "SW", "master-data", "CLIENT",
                UUID.fromString("6a7c7f1b-a61f-4f4b-96de-6748647735c1"),
                SyncOperation.UPSERT, 3, Instant.EPOCH, Instant.EPOCH,
                objectMapper.createObjectNode().put("first_name", "Synthetic").put("last_name", "Client"));
    }
}
