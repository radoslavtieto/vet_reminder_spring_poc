package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.domain.SnapshotProgress;
import com.vetsync.sync.domain.SnapshotProgressState;
import com.vetsync.sync.domain.SnapshotRecord;
import com.vetsync.sync.domain.SnapshotMode;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.sync.domain.SnapshotSyncResult;
import com.vetsync.sync.domain.SnapshotSyncStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SnapshotSyncUseCase {

    private final SnapshotSource snapshotSource;
    private final SnapshotProgressRepository progressRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final LeaseRepository leaseRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public SnapshotSyncUseCase(
            SnapshotSource snapshotSource,
            SnapshotProgressRepository progressRepository,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock clock) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource, "snapshotSource");
        this.progressRepository = Objects.requireNonNull(progressRepository, "progressRepository");
        this.processedEventRepository = Objects.requireNonNull(processedEventRepository, "processedEventRepository");
        this.leaseRepository = Objects.requireNonNull(leaseRepository, "leaseRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SnapshotSyncResult execute(SnapshotSyncCommand command) {
        Objects.requireNonNull(command, "command");
        String scope = "snapshot:" + command.entity().contractName() + ":" + command.progressScope();
        Instant leaseNow = clock.instant();
        if (!leaseRepository.tryAcquire(
                command.tenant(), scope, command.leaseOwner(), leaseNow, leaseNow.plus(command.leaseDuration()))) {
            return new SnapshotSyncResult(SnapshotSyncStatus.SKIPPED_LEASED, 0, 0, 0, 0);
        }
        try {
            return syncWhileLeased(command, scope);
        } finally {
            leaseRepository.release(command.tenant(), scope, command.leaseOwner());
        }
    }

    private SnapshotSyncResult syncWhileLeased(SnapshotSyncCommand command, String scope) {
        SnapshotProgress progress = progressRepository.find(
                        command.tenant(), command.entity(), command.progressScope())
                .orElseGet(() -> new SnapshotProgress(
                        command.tenant(), command.entity(), command.progressScope(), 0, SnapshotProgressState.IN_PROGRESS));
        if (progress.tenant() != command.tenant()
                || progress.entity() != command.entity()
                || !progress.runId().equals(command.progressScope())) {
            throw new IllegalStateException("Stored snapshot progress does not match the requested run");
        }
        if (progress.state() == SnapshotProgressState.DONE) {
            return new SnapshotSyncResult(
                    SnapshotSyncStatus.ALREADY_COMPLETED, 0, 0, 0, progress.lastId());
        }
        progressRepository.save(progress);

        int readCount = 0;
        int publishedCount = 0;
        int duplicateCount = 0;
        long lastId = progress.lastId();
        while (true) {
            List<SnapshotRecord> records = Objects.requireNonNull(
                    snapshotSource.findSnapshotPage(command, lastId), "snapshotSource returned null");
            if (records.size() > command.batchSize()) {
                throw new IllegalStateException("Snapshot source returned more rows than the requested batch size");
            }
            validatePage(records, lastId, command);
            if (records.isEmpty()) {
                progress = new SnapshotProgress(
                        command.tenant(), command.entity(), command.progressScope(), lastId, SnapshotProgressState.DONE);
                progressRepository.save(progress);
                break;
            }

            for (SnapshotRecord record : records) {
                renewLease(command, scope);
                String key = idempotencyKey(command, record);
                if (processedEventRepository.contains(key)) {
                    duplicateCount++;
                } else {
                    eventPublisher.publish(toEnvelope(command, record, key));
                    processedEventRepository.markProcessed(key, clock.instant());
                    publishedCount++;
                }
                lastId = record.sourceId();
                progress = new SnapshotProgress(
                        command.tenant(), command.entity(), command.progressScope(),
                        lastId, SnapshotProgressState.IN_PROGRESS);
                progressRepository.save(progress);
                readCount++;
            }
            if (records.size() < command.batchSize()) {
                progress = new SnapshotProgress(
                        command.tenant(), command.entity(), command.progressScope(), lastId, SnapshotProgressState.DONE);
                progressRepository.save(progress);
                break;
            }
        }
        return new SnapshotSyncResult(SnapshotSyncStatus.COMPLETED, readCount, publishedCount, duplicateCount, lastId);
    }

    private void validatePage(List<SnapshotRecord> records, long afterSourceId, SnapshotSyncCommand command) {
        long previousId = afterSourceId;
        for (SnapshotRecord record : records) {
            if (record.sourceId() <= previousId) {
                throw new IllegalStateException("Snapshot records must have strictly increasing sourceId");
            }
            if (!command.entity().entityType().equals(record.entityType())) {
                throw new IllegalStateException("Unexpected entity type in " + command.entity().contractName()
                        + " snapshot: " + record.entityType());
            }
            previousId = record.sourceId();
        }
    }

    private void renewLease(SnapshotSyncCommand command, String scope) {
        Instant now = clock.instant();
        if (!leaseRepository.renew(
                command.tenant(), scope, command.leaseOwner(), now, now.plus(command.leaseDuration()))) {
            throw new IllegalStateException(
                    "Lost synchronization lease for " + command.tenant() + ":" + scope);
        }
    }

    private String idempotencyKey(SnapshotSyncCommand command, SnapshotRecord record) {
        String baseKey = command.tenant().name() + ":" + record.entityType() + ":" + record.publicId() + ":"
                + record.sourceVersion();
        return command.mode() == SnapshotMode.INITIAL
                ? baseKey
                : baseKey + ":snapshot:" + command.progressScope();
    }

    private SyncEnvelope toEnvelope(
            SnapshotSyncCommand command,
            SnapshotRecord record,
            String idempotencyKey) {
        UUID eventId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
        return new SyncEnvelope(
                SyncEnvelope.CURRENT_SCHEMA_VERSION,
                eventId,
                eventId,
                command.tenant().name(),
                command.entity().stream().contractName(),
                record.entityType(),
                record.publicId(),
                record.operation(),
                record.sourceVersion(),
                record.occurredAt(),
                clock.instant(),
                record.payload());
    }
}
