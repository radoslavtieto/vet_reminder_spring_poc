package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.domain.AuditChange;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SyncPointer;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollStreamUseCaseTests {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final PollCommand COMMAND = new PollCommand(
            TenantCode.SW, SyncStream.MASTER_DATA, 10, Duration.ofSeconds(5), 2,
            "worker-1", Duration.ofMinutes(5));

    @Test
    void publishesAuditChangeAndAdvancesPointerAfterPublish() {
        InMemoryPorts ports = new InMemoryPorts(List.of(change(11, NOW.minusSeconds(6))));
        EventPublisher publisher = envelope -> ports.operations.add("publish");

        PollResult result = useCase(ports, publisher).execute(COMMAND);

        assertThat(result.status()).isEqualTo(PollStatus.COMPLETED);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.lastAuditId()).isEqualTo(11);
        assertThat(ports.operations).containsExactly(
                "acquire-lease", "renew-lease", "publish", "mark-processed", "save-pointer", "release-lease");
        assertThat(ports.pointer.lastAuditId()).isEqualTo(11);
    }

    @Test
    void doesNotMarkOrAdvancePointerWhenPublishingFails() {
        InMemoryPorts ports = new InMemoryPorts(List.of(change(11, NOW.minusSeconds(6))));

        assertThatThrownBy(() -> useCase(ports, envelope -> {
            throw new IllegalStateException("broker unavailable");
        }).execute(COMMAND))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.pointer.lastAuditId()).isZero();
        assertThat(ports.operations).containsExactly("acquire-lease", "renew-lease", "release-lease");
    }

    @Test
    void skipsAlreadyProcessedAuditRowsAndStillMovesPastThem() {
        AuditChange change = change(11, NOW.minusSeconds(6));
        InMemoryPorts ports = new InMemoryPorts(List.of(change));
        ports.processedKeys.add(change.idempotencyKey(TenantCode.SW));
        List<SyncEnvelope> published = new ArrayList<>();

        PollResult result = useCase(ports, published::add).execute(COMMAND);

        assertThat(result.publishedCount()).isZero();
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(ports.pointer.lastAuditId()).isEqualTo(11);
        assertThat(published).isEmpty();
    }

    @Test
    void queriesWithSafetyLagAndReplaysOverlapForLateAuditCommits() {
        InMemoryPorts ports = new InMemoryPorts(List.of(change(9, NOW.minusSeconds(6))));
        ports.pointer = new SyncPointer(TenantCode.SW, SyncStream.MASTER_DATA, 10);

        PollResult result = useCase(ports, envelope -> { }).execute(COMMAND);

        assertThat(ports.requestedFromAuditId).isEqualTo(9);
        assertThat(ports.requestedChangedBefore).isEqualTo(NOW.minusSeconds(5));
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(ports.pointer.lastAuditId()).isEqualTo(10);
    }

    @Test
    void rejectsUnorderedSourceRowsInsteadOfAdvancingPastUnseenChanges() {
        InMemoryPorts ports = new InMemoryPorts(List.of(
                change(12, NOW.minusSeconds(6)),
                change(11, NOW.minusSeconds(6))));

        assertThatThrownBy(() -> useCase(ports, envelope -> { }).execute(COMMAND))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordered by strictly increasing auditId");
        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.operations).containsExactly("acquire-lease", "release-lease");
        assertThat(ports.pointer.lastAuditId()).isZero();
    }

    @Test
    void rejectsOverlapLargeEnoughToPreventCursorProgress() {
        assertThatThrownBy(() -> new PollCommand(
                TenantCode.SW, SyncStream.MASTER_DATA, 2, Duration.ZERO, 2,
                "worker-1", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overlapAuditRows must be smaller than batchSize");
    }

    @Test
    void skipsPollWhenAnotherWorkerOwnsTheLease() {
        InMemoryPorts ports = new InMemoryPorts(List.of(change(11, NOW.minusSeconds(6))));
        ports.leaseAvailable = false;

        PollResult result = useCase(ports, envelope -> { }).execute(COMMAND);

        assertThat(result.status()).isEqualTo(PollStatus.SKIPPED_LEASED);
        assertThat(result.readCount()).isZero();
        assertThat(ports.requestedFromAuditId).isZero();
        assertThat(ports.operations).containsExactly("acquire-lease");
    }

    @Test
    void stopsPublishingWhenLeaseRenewalFails() {
        InMemoryPorts ports = new InMemoryPorts(List.of(change(11, NOW.minusSeconds(6))));
        ports.leaseRenewalAvailable = false;

        assertThatThrownBy(() -> useCase(ports, envelope -> {
            throw new AssertionError("event must not publish after lease loss");
        }).execute(COMMAND))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lost synchronization lease");

        assertThat(ports.pointer.lastAuditId()).isZero();
        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.operations).containsExactly("acquire-lease", "renew-lease", "release-lease");
    }

    private static PollStreamUseCase useCase(InMemoryPorts ports, EventPublisher publisher) {
        return new PollStreamUseCase(
                ports,
                ports,
                ports,
                ports,
                publisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AuditChange change(long auditId, Instant changedAt) {
        return new AuditChange(
                auditId,
                "CLIENT",
                UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c"),
                com.vetsync.contracts.sync.SyncOperation.UPSERT,
                3,
                changedAt,
                Map.of("name", "Synthetic Client"));
    }

    private static final class InMemoryPorts
            implements AuditSource, PointerRepository, ProcessedEventRepository, LeaseRepository {

        private final List<AuditChange> changes;
        private final Set<String> processedKeys = new HashSet<>();
        private final List<String> operations = new ArrayList<>();
        private SyncPointer pointer = new SyncPointer(TenantCode.SW, SyncStream.MASTER_DATA, 0);
        private boolean leaseAvailable = true;
        private boolean leaseRenewalAvailable = true;
        private long requestedFromAuditId;
        private Instant requestedChangedBefore;

        private InMemoryPorts(List<AuditChange> changes) {
            this.changes = changes;
        }

        @Override
        public List<AuditChange> findChanges(
                TenantCode tenant,
                SyncStream stream,
                long fromAuditIdInclusive,
                Instant changedBefore,
                int limit) {
            requestedFromAuditId = fromAuditIdInclusive;
            requestedChangedBefore = changedBefore;
            return changes.stream()
                    .filter(change -> change.auditId() >= fromAuditIdInclusive)
                    .filter(change -> change.changedAt().isBefore(changedBefore))
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<SyncPointer> find(TenantCode tenant, SyncStream stream) {
            return Optional.of(pointer);
        }

        @Override
        public void save(SyncPointer updatedPointer) {
            operations.add("save-pointer");
            pointer = updatedPointer;
        }

        @Override
        public boolean contains(String idempotencyKey) {
            return processedKeys.contains(idempotencyKey);
        }

        @Override
        public void markProcessed(String idempotencyKey, Instant processedAt) {
            operations.add("mark-processed");
            processedKeys.add(idempotencyKey);
        }

        @Override
        public boolean tryAcquire(
                TenantCode tenant,
                String scope,
                String owner,
                Instant now,
                Instant expiresAt) {
            operations.add("acquire-lease");
            return leaseAvailable;
        }

        @Override
        public boolean renew(
                TenantCode tenant,
                String scope,
                String owner,
                Instant now,
                Instant expiresAt) {
            operations.add("renew-lease");
            return leaseAvailable && leaseRenewalAvailable;
        }

        @Override
        public void release(TenantCode tenant, String scope, String owner) {
            operations.add("release-lease");
        }
    }
}
