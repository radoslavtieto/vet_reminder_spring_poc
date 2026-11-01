package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.domain.AuditChange;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SyncPointer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PollStreamUseCase {

    private final AuditSource auditSource;
    private final PointerRepository pointerRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final LeaseRepository leaseRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public PollStreamUseCase(
            AuditSource auditSource,
            PointerRepository pointerRepository,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock clock) {
        this.auditSource = Objects.requireNonNull(auditSource, "auditSource");
        this.pointerRepository = Objects.requireNonNull(pointerRepository, "pointerRepository");
        this.processedEventRepository = Objects.requireNonNull(processedEventRepository, "processedEventRepository");
        this.leaseRepository = Objects.requireNonNull(leaseRepository, "leaseRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PollResult execute(PollCommand command) {
        Objects.requireNonNull(command, "command");
        Instant leaseNow = clock.instant();
        if (!leaseRepository.tryAcquire(
                command.tenant(),
                command.stream().contractName(),
                command.leaseOwner(),
                leaseNow,
                leaseNow.plus(command.leaseDuration()))) {
            return new PollResult(PollStatus.SKIPPED_LEASED, 0, 0, 0, 0);
        }

        try {
            return pollWhileLeased(command);
        } finally {
            leaseRepository.release(command.tenant(), command.stream().contractName(), command.leaseOwner());
        }
    }

    private PollResult pollWhileLeased(PollCommand command) {
        SyncPointer currentPointer = pointerRepository.find(command.tenant(), command.stream())
                .orElse(new SyncPointer(command.tenant(), command.stream(), 0));
        if (currentPointer.tenant() != command.tenant() || currentPointer.stream() != command.stream()) {
            throw new IllegalStateException("Stored pointer does not match the requested tenant and stream");
        }

        long fromAuditId = Math.max(1, currentPointer.lastAuditId() - command.overlapAuditRows() + 1);
        Instant changedBefore = clock.instant().minus(command.safetyLag());
        List<AuditChange> changes = Objects.requireNonNull(
                auditSource.findChanges(
                        command.tenant(),
                        command.stream(),
                        fromAuditId,
                        changedBefore,
                        command.batchSize()),
                "auditSource returned null");
        if (changes.size() > command.batchSize()) {
            throw new IllegalStateException("Audit source returned more rows than the requested batch size");
        }
        validateChanges(changes, fromAuditId);

        int publishedCount = 0;
        int duplicateCount = 0;
        long lastAuditId = currentPointer.lastAuditId();
        for (AuditChange change : changes) {
            renewLease(command);

            String idempotencyKey = change.idempotencyKey(command.tenant());
            if (processedEventRepository.contains(idempotencyKey)) {
                duplicateCount++;
            } else {
                eventPublisher.publish(toEnvelope(command, change, idempotencyKey));
                processedEventRepository.markProcessed(idempotencyKey, clock.instant());
                publishedCount++;
            }
            lastAuditId = Math.max(lastAuditId, change.auditId());
        }

        if (lastAuditId > currentPointer.lastAuditId()) {
            pointerRepository.save(new SyncPointer(command.tenant(), command.stream(), lastAuditId));
        }
        return new PollResult(PollStatus.COMPLETED, changes.size(), publishedCount, duplicateCount, lastAuditId);
    }

    private void validateChanges(List<AuditChange> changes, long fromAuditId) {
        long previousAuditId = fromAuditId - 1;
        for (AuditChange change : changes) {
            if (change.auditId() < fromAuditId || change.auditId() <= previousAuditId) {
                throw new IllegalStateException("Audit rows must be ordered by strictly increasing auditId");
            }
            previousAuditId = change.auditId();
        }
    }

    private void renewLease(PollCommand command) {
        Instant now = clock.instant();
        if (!leaseRepository.renew(
                command.tenant(),
                command.stream().contractName(),
                command.leaseOwner(),
                now,
                now.plus(command.leaseDuration()))) {
            throw new IllegalStateException("Lost synchronization lease for "
                    + command.tenant() + ":" + command.stream().contractName());
        }
    }

    private SyncEnvelope toEnvelope(PollCommand command, AuditChange change, String idempotencyKey) {
        UUID eventId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
        Instant publishedAt = clock.instant();
        return new SyncEnvelope(
                SyncEnvelope.CURRENT_SCHEMA_VERSION,
                eventId,
                eventId,
                command.tenant().name(),
                command.stream().contractName(),
                change.entityType(),
                change.publicId(),
                change.operation(),
                change.sourceVersion(),
                change.changedAt(),
                publishedAt,
                change.payload());
    }
}
