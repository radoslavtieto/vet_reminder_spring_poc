package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.contracts.sync.SyncOperation;
import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotMode;
import com.vetsync.sync.domain.SnapshotProgress;
import com.vetsync.sync.domain.SnapshotProgressState;
import com.vetsync.sync.domain.SnapshotRecord;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.sync.domain.SnapshotSyncResult;
import com.vetsync.sync.domain.SnapshotSyncStatus;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotSyncUseCaseTests {

    private static final Instant NOW = Instant.parse("2026-10-30T22:30:00Z");
    private static final UUID FIRST_PUBLIC_ID = UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c");
    private static final UUID SECOND_PUBLIC_ID = UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c");

    @Test
    void publishesEachPageAndMarksInitialSyncDoneAfterAllRows() {
        InMemoryPorts ports = new InMemoryPorts(List.of(
                record(1, FIRST_PUBLIC_ID, SyncOperation.UPSERT),
                record(2, SECOND_PUBLIC_ID, SyncOperation.DELETE)));
        List<SyncEnvelope> published = new ArrayList<>();

        SnapshotSyncResult result = useCase(ports, envelope -> {
            ports.operations.add("publish");
            published.add(envelope);
        }).execute(initialCommand());

        assertThat(result.status()).isEqualTo(SnapshotSyncStatus.COMPLETED);
        assertThat(result.readCount()).isEqualTo(2);
        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(published).extracting(SyncEnvelope::operation)
                .containsExactly(SyncOperation.UPSERT, SyncOperation.DELETE);
        assertThat(ports.progress.get("SW:client:initial"))
                .isEqualTo(new SnapshotProgress(
                        TenantCode.SW, SnapshotEntity.CLIENT, "initial", 2, SnapshotProgressState.DONE));
        assertThat(ports.processedKeys).contains(
                "SW:CLIENT:" + FIRST_PUBLIC_ID + ":7",
                "SW:CLIENT:" + SECOND_PUBLIC_ID + ":7");
        assertThat(ports.operations).containsSubsequence("publish", "mark", "save-progress");
    }

    @Test
    void resumesFromPersistedKeysetCursorAfterAWorkerRestart() {
        InMemoryPorts ports = new InMemoryPorts(List.of(
                record(1, FIRST_PUBLIC_ID, SyncOperation.UPSERT),
                record(2, SECOND_PUBLIC_ID, SyncOperation.UPSERT)));
        ports.progress.put("SW:client:initial", new SnapshotProgress(
                TenantCode.SW, SnapshotEntity.CLIENT, "initial", 1, SnapshotProgressState.IN_PROGRESS));
        List<SyncEnvelope> published = new ArrayList<>();

        SnapshotSyncResult result = useCase(ports, published::add).execute(initialCommand());

        assertThat(result.readCount()).isEqualTo(1);
        assertThat(published).singleElement().extracting(SyncEnvelope::entityId).isEqualTo(SECOND_PUBLIC_ID);
        assertThat(ports.lastSourceCursor).isEqualTo(2);
        assertThat(ports.progress.get("SW:client:initial").state()).isEqualTo(SnapshotProgressState.DONE);
    }

    @Test
    void failedPublicationDoesNotMarkOrAdvanceTheSnapshotCursor() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(1, FIRST_PUBLIC_ID, SyncOperation.UPSERT)));

        assertThatThrownBy(() -> useCase(ports, envelope -> {
            throw new IllegalStateException("broker unavailable");
        }).execute(initialCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.progress.get("SW:client:initial").lastId()).isZero();
        assertThat(ports.operations).contains("release-lease");
    }

    @Test
    void routesPartialRunFilterAndUsesRunScopedDedupeKey() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(5, FIRST_PUBLIC_ID, SyncOperation.UPSERT)));
        SnapshotSyncCommand command = new SnapshotSyncCommand(
                TenantCode.SW,
                SnapshotEntity.CLIENT,
                SnapshotMode.PARTIAL,
                "replay-2026-10-31",
                FIRST_PUBLIC_ID,
                null,
                null,
                2,
                "worker-1",
                Duration.ofMinutes(5));

        SnapshotSyncResult result = useCase(ports, envelope -> {
        }).execute(command);

        assertThat(result.readCount()).isEqualTo(1);
        assertThat(ports.lastCommand).isEqualTo(command);
        assertThat(ports.processedKeys).contains(
                "SW:CLIENT:" + FIRST_PUBLIC_ID + ":7:snapshot:partial:replay-2026-10-31");
    }

    @Test
    void doesNotRepeatCompletedInitialSnapshot() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(1, FIRST_PUBLIC_ID, SyncOperation.UPSERT)));
        ports.progress.put("SW:client:initial", new SnapshotProgress(
                TenantCode.SW, SnapshotEntity.CLIENT, "initial", 1, SnapshotProgressState.DONE));

        SnapshotSyncResult result = useCase(ports, envelope -> {
            throw new AssertionError("completed snapshot must not republish");
        }).execute(initialCommand());

        assertThat(result.status()).isEqualTo(SnapshotSyncStatus.ALREADY_COMPLETED);
        assertThat(result.readCount()).isZero();
    }

    @Test
    void stopsSnapshotWhenLeaseIsLostBeforePublishing() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(1, FIRST_PUBLIC_ID, SyncOperation.UPSERT)));
        ports.leaseRenewalAvailable = false;

        assertThatThrownBy(() -> useCase(ports, envelope -> {
        }).execute(initialCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lost synchronization lease");

        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.progress.get("SW:client:initial").lastId()).isZero();
        assertThat(ports.operations).contains("release-lease");
    }

    private static SnapshotSyncUseCase useCase(InMemoryPorts ports, EventPublisher publisher) {
        return new SnapshotSyncUseCase(
                ports,
                ports,
                ports,
                ports,
                publisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static SnapshotSyncCommand initialCommand() {
        return new SnapshotSyncCommand(
                TenantCode.SW,
                SnapshotEntity.CLIENT,
                SnapshotMode.INITIAL,
                "initial",
                null,
                null,
                null,
                1,
                "worker-1",
                Duration.ofMinutes(5));
    }

    private static SnapshotRecord record(long sourceId, UUID publicId, SyncOperation operation) {
        return new SnapshotRecord(
                sourceId,
                publicId,
                "CLIENT",
                7,
                NOW.minusSeconds(sourceId),
                operation,
                Map.of("id", sourceId, "tenant_id", "SW"));
    }

    private static final class InMemoryPorts implements
            SnapshotSource,
            SnapshotProgressRepository,
            ProcessedEventRepository,
            LeaseRepository {

        private final List<SnapshotRecord> records;
        private final Map<String, SnapshotProgress> progress = new HashMap<>();
        private final Set<String> processedKeys = new HashSet<>();
        private final List<String> operations = new ArrayList<>();
        private SnapshotSyncCommand lastCommand;
        private long lastSourceCursor;
        private boolean leaseRenewalAvailable = true;

        private InMemoryPorts(List<SnapshotRecord> records) {
            this.records = records;
        }

        @Override
        public List<SnapshotRecord> findSnapshotPage(SnapshotSyncCommand command, long afterSourceId) {
            lastCommand = command;
            lastSourceCursor = afterSourceId;
            return records.stream()
                    .filter(record -> record.sourceId() > afterSourceId)
                    .limit(command.batchSize())
                    .toList();
        }

        @Override
        public java.util.Optional<SnapshotProgress> find(
                TenantCode tenant,
                SnapshotEntity entity,
                String runId) {
            return java.util.Optional.ofNullable(progress.get(progressKey(tenant, entity, runId)));
        }

        @Override
        public void save(SnapshotProgress snapshotProgress) {
            operations.add("save-progress");
            progress.put(progressKey(snapshotProgress.tenant(), snapshotProgress.entity(), snapshotProgress.runId()),
                    snapshotProgress);
        }

        @Override
        public boolean contains(String idempotencyKey) {
            return processedKeys.contains(idempotencyKey);
        }

        @Override
        public void markProcessed(String idempotencyKey, Instant processedAt) {
            operations.add("mark");
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
            return true;
        }

        @Override
        public boolean renew(
                TenantCode tenant,
                String scope,
                String owner,
                Instant now,
                Instant expiresAt) {
            return leaseRenewalAvailable;
        }

        @Override
        public void release(TenantCode tenant, String scope, String owner) {
            operations.add("release-lease");
        }

        private static String progressKey(TenantCode tenant, SnapshotEntity entity, String runId) {
            return tenant + ":" + entity.contractName() + ":" + runId;
        }
    }
}
