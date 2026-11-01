package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.contracts.sync.SyncOperation;
import com.vetsync.sync.domain.TimeWindowRecord;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import com.vetsync.sync.domain.TimeWindowScanResult;
import com.vetsync.sync.domain.TimeWindowScanStatus;
import com.vetsync.sync.domain.SyncStream;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TimeWindowScanUseCase {

    private final TimeWindowSource timeWindowSource;
    private final ProcessedEventRepository processedEventRepository;
    private final LeaseRepository leaseRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public TimeWindowScanUseCase(
            TimeWindowSource timeWindowSource,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock clock) {
        this.timeWindowSource = Objects.requireNonNull(timeWindowSource, "timeWindowSource");
        this.processedEventRepository = Objects.requireNonNull(
                processedEventRepository, "processedEventRepository");
        this.leaseRepository = Objects.requireNonNull(leaseRepository, "leaseRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TimeWindowScanResult execute(TimeWindowScanCommand command) {
        Objects.requireNonNull(command, "command");
        Instant leaseNow = clock.instant();
        String scope = command.stream().contractName();
        if (!leaseRepository.tryAcquire(
                command.tenant(),
                scope,
                command.leaseOwner(),
                leaseNow,
                leaseNow.plus(command.leaseDuration()))) {
            return new TimeWindowScanResult(TimeWindowScanStatus.SKIPPED_LEASED, 0, 0, 0);
        }

        try {
            return scanWhileLeased(command, scope);
        } finally {
            leaseRepository.release(command.tenant(), scope, command.leaseOwner());
        }
    }

    private TimeWindowScanResult scanWhileLeased(TimeWindowScanCommand command, String scope) {
        LocalDate today = LocalDate.now(clock);
        long afterSourceId = 0;
        int readCount = 0;
        int publishedCount = 0;
        int duplicateCount = 0;

        while (true) {
            List<TimeWindowRecord> records = Objects.requireNonNull(
                    timeWindowSource.findEligible(command, today, afterSourceId),
                    "timeWindowSource returned null");
            if (records.size() > command.batchSize()) {
                throw new IllegalStateException("Time-window source returned more rows than the requested batch size");
            }
            validatePage(records, afterSourceId, command);
            if (records.isEmpty()) {
                break;
            }

            for (TimeWindowRecord record : records) {
                renewLease(command, scope);
                String key = idempotencyKey(command, record, today);
                if (processedEventRepository.contains(key)) {
                    duplicateCount++;
                } else {
                    eventPublisher.publish(toEnvelope(command, record, key));
                    processedEventRepository.markProcessed(key, clock.instant());
                    publishedCount++;
                }
                readCount++;
            }
            afterSourceId = records.get(records.size() - 1).sourceId();
            if (records.size() < command.batchSize()) {
                break;
            }
        }
        return new TimeWindowScanResult(
                TimeWindowScanStatus.COMPLETED, readCount, publishedCount, duplicateCount);
    }

    private void validatePage(
            List<TimeWindowRecord> records,
            long afterSourceId,
            TimeWindowScanCommand command) {
        long previousId = afterSourceId;
        String requiredEntityType = command.stream() == SyncStream.PRESCRIPTION_EXPIRY
                ? "PRESCRIPTION"
                : "INVOICE";
        for (TimeWindowRecord record : records) {
            if (record.sourceId() <= previousId) {
                throw new IllegalStateException("Time-window records must have strictly increasing sourceId");
            }
            if (!requiredEntityType.equals(record.entityType())) {
                throw new IllegalStateException("Unexpected entity type in " + command.stream().contractName()
                        + " scan: " + record.entityType());
            }
            previousId = record.sourceId();
        }
    }

    private void renewLease(TimeWindowScanCommand command, String scope) {
        Instant now = clock.instant();
        if (!leaseRepository.renew(
                command.tenant(),
                scope,
                command.leaseOwner(),
                now,
                now.plus(command.leaseDuration()))) {
            throw new IllegalStateException("Lost synchronization lease for "
                    + command.tenant() + ":" + scope);
        }
    }

    private String idempotencyKey(
            TimeWindowScanCommand command,
            TimeWindowRecord record,
            LocalDate today) {
        return command.tenant().name() + ":" + record.entityType() + ":" + record.publicId() + ":"
                + record.sourceVersion() + ":" + command.stream().contractName() + ":" + today;
    }

    private SyncEnvelope toEnvelope(
            TimeWindowScanCommand command,
            TimeWindowRecord record,
            String idempotencyKey) {
        UUID eventId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
        SyncOperation operation = command.stream().contractName().equals("prescription-expiry")
                ? SyncOperation.EXPIRY_DUE
                : SyncOperation.OVERDUE;
        return new SyncEnvelope(
                SyncEnvelope.CURRENT_SCHEMA_VERSION,
                eventId,
                eventId,
                command.tenant().name(),
                command.stream().contractName(),
                record.entityType(),
                record.publicId(),
                operation,
                record.sourceVersion(),
                record.occurredAt(),
                clock.instant(),
                record.payload());
    }
}
