package com.vetsync.sync.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vetsync.sync.domain.AuditChange;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

class PollMasterDataUseCaseTests {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private final AuditChangeSource source = mock(AuditChangeSource.class);
    private final SyncProgressStore progress = mock(SyncProgressStore.class);
    private final SyncEventPublisher publisher = mock(SyncEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PollMasterDataUseCase useCase = new PollMasterDataUseCase(
            source, progress, publisher, objectMapper,
            Clock.fixed(NOW, ZoneOffset.UTC), List.of("SW"),
            Duration.ofSeconds(5), Duration.ofSeconds(10), 50);

    @Test
    void advancesThePointerOnlyAfterPublishingTheMappedEvent() {
        AuditChange change = auditChange();
        when(progress.lastAuditId("SW", "master-data")).thenReturn(7L);
        when(source.readMasterData("SW", 7L, NOW.minusSeconds(15), NOW.minusSeconds(5), 50))
                .thenReturn(List.of(change));

        assertThat(useCase.poll()).isEqualTo(1);

        verify(publisher).publish(eq(useCase.envelopeFor(change)));
        verify(progress).advance("SW", "master-data", 8L);
    }

    @Test
    void leavesThePointerUnchangedWhenPublishingFails() {
        AuditChange change = auditChange();
        when(progress.lastAuditId("SW", "master-data")).thenReturn(7L);
        when(source.readMasterData("SW", 7L, NOW.minusSeconds(15), NOW.minusSeconds(5), 50))
                .thenReturn(List.of(change));
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(publisher).publish(any());

        assertThatThrownBy(useCase::poll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        verify(progress, never()).advance(any(), any(), anyLong());
    }

    private AuditChange auditChange() {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("first_name", "Synthetic")
                .put("last_name", "Client");
        return new AuditChange(
                8,
                "SW",
                "client",
                UUID.fromString("6a7c7f1b-a61f-4f4b-96de-6748647735c1"),
                "UPDATE",
                3,
                NOW.minusSeconds(30),
                payload
        );
    }
}
