package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.PollStreamUseCase;
import com.vetsync.sync.application.SnapshotSyncUseCase;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.application.TimeWindowScanUseCase;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotMode;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.sync.domain.SnapshotSyncResult;
import com.vetsync.sync.domain.SnapshotSyncStatus;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import com.vetsync.sync.config.SyncPollProperties;
import com.vetsync.sync.config.TimeWindowScanProperties;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import com.vetsync.sync.domain.TimeWindowScanResult;
import com.vetsync.sync.domain.TimeWindowScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.Instant;

public final class PollJobRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollJobRunner.class);

    private final PollStreamUseCase pollStreamUseCase;
    private final TimeWindowScanUseCase timeWindowScanUseCase;
    private final SnapshotSyncUseCase snapshotSyncUseCase;
    private final SyncRunTracker syncRunTracker;
    private final SyncPollProperties pollProperties;
    private final TimeWindowScanProperties scanProperties;
    private final String owner = UUID.randomUUID().toString();

    public PollJobRunner(
            PollStreamUseCase pollStreamUseCase,
            TimeWindowScanUseCase timeWindowScanUseCase,
            SnapshotSyncUseCase snapshotSyncUseCase,
            SyncRunTracker syncRunTracker,
            SyncPollProperties pollProperties,
            TimeWindowScanProperties scanProperties) {
        this.pollStreamUseCase = Objects.requireNonNull(pollStreamUseCase, "pollStreamUseCase");
        this.timeWindowScanUseCase = Objects.requireNonNull(timeWindowScanUseCase, "timeWindowScanUseCase");
        this.snapshotSyncUseCase = Objects.requireNonNull(snapshotSyncUseCase, "snapshotSyncUseCase");
        this.syncRunTracker = Objects.requireNonNull(syncRunTracker, "syncRunTracker");
        this.pollProperties = Objects.requireNonNull(pollProperties, "pollProperties");
        this.scanProperties = Objects.requireNonNull(scanProperties, "scanProperties");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        String operation = optionalOption(arguments, "sync.operation");
        if (operation != null) {
            runSnapshot(arguments, operation);
            return;
        }
        String streamValue = requiredOption(arguments, "sync.stream");
        String tenantValue = requiredOption(arguments, "sync.tenant");
        SyncStream stream = SyncStream.fromContractName(streamValue);

        List<TenantCode> tenants = tenantValue.equalsIgnoreCase("ALL")
                ? List.of(TenantCode.values())
                : List.of(parseTenant(tenantValue));
        RuntimeException firstFailure = null;
        for (TenantCode tenant : tenants) {
            try {
                if (stream.isAuditStream()) {
                    PollResult result = syncRunTracker.track(
                            tenant, stream.contractName(), () -> pollStreamUseCase.execute(auditCommand(tenant, stream)));
                    if (result.status() == PollStatus.SKIPPED_LEASED) {
                        LOGGER.info("Skipping sync job for {}:{} because its lease is held",
                                tenant, stream.contractName());
                    } else {
                        LOGGER.info("Completed sync job for {}:{}; read={}, published={}, duplicates={}",
                                tenant, stream.contractName(), result.readCount(),
                                result.publishedCount(), result.duplicateCount());
                    }
                } else {
                    TimeWindowScanResult result = syncRunTracker.track(
                            tenant,
                            stream.contractName(),
                            () -> timeWindowScanUseCase.execute(scanCommand(tenant, stream)));
                    if (result.status() == TimeWindowScanStatus.SKIPPED_LEASED) {
                        LOGGER.info("Skipping sync job for {}:{} because its lease is held",
                                tenant, stream.contractName());
                    } else {
                        LOGGER.info("Completed sync job for {}:{}; read={}, published={}, duplicates={}",
                                tenant, stream.contractName(), result.readCount(),
                                result.publishedCount(), result.duplicateCount());
                    }
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Sync job failed for {}:{}", tenant, stream.contractName(), exception);
                if (firstFailure == null) {
                    firstFailure = exception;
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        if (firstFailure != null) {
            throw new IllegalStateException("One or more tenant sync jobs failed", firstFailure);
        }
    }

    private void runSnapshot(ApplicationArguments arguments, String operationValue) {
        SnapshotMode mode = switch (operationValue.toLowerCase(java.util.Locale.ROOT)) {
            case "initial" -> SnapshotMode.INITIAL;
            case "partial" -> SnapshotMode.PARTIAL;
            default -> throw new IllegalArgumentException(
                    "Unsupported sync operation: " + operationValue + "; expected initial or partial");
        };
        SnapshotEntity entity = SnapshotEntity.fromContractName(requiredOption(arguments, "sync.entity"));
        String tenantValue = requiredOption(arguments, "sync.tenant");
        List<TenantCode> tenants = tenantValue.equalsIgnoreCase("ALL")
                ? List.of(TenantCode.values())
                : List.of(parseTenant(tenantValue));
        String runId = mode == SnapshotMode.INITIAL ? "initial" : requiredOption(arguments, "sync.run-id");
        UUID publicId = optionalOption(arguments, "sync.public-id") == null
                ? null
                : parsePublicId(requiredOption(arguments, "sync.public-id"));
        Instant updatedFrom = parseInstant(optionalOption(arguments, "sync.updated-from"), "sync.updated-from");
        Instant updatedTo = parseInstant(optionalOption(arguments, "sync.updated-to"), "sync.updated-to");
        if (mode == SnapshotMode.INITIAL
                && (publicId != null || updatedFrom != null || updatedTo != null)) {
            throw new IllegalArgumentException("Initial snapshot does not accept partial-sync filters");
        }
        SnapshotSyncCommand template = new SnapshotSyncCommand(
                tenants.get(0),
                entity,
                mode,
                runId,
                publicId,
                updatedFrom,
                updatedTo,
                pollProperties.batchSize(),
                owner,
                pollProperties.leaseDuration());
        RuntimeException firstFailure = null;
        for (TenantCode tenant : tenants) {
            try {
                SnapshotSyncResult result = syncRunTracker.track(
                        tenant,
                        "snapshot:" + entity.contractName() + ":" + template.progressScope(),
                        () -> snapshotSyncUseCase.execute(new SnapshotSyncCommand(
                                tenant,
                                template.entity(),
                                template.mode(),
                                template.runId(),
                                template.publicId(),
                                template.updatedFromInclusive(),
                                template.updatedToExclusive(),
                                template.batchSize(),
                                template.leaseOwner(),
                                template.leaseDuration())));
                if (result.status() == SnapshotSyncStatus.SKIPPED_LEASED) {
                    LOGGER.info("Skipping {} snapshot for {} because its lease is held", entity.contractName(), tenant);
                } else {
                    LOGGER.info("Snapshot {} for {} finished with {}; read={}, published={}, duplicates={}, lastId={}",
                            entity.contractName(), tenant, result.status(), result.readCount(),
                            result.publishedCount(), result.duplicateCount(), result.lastId());
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Snapshot {} failed for tenant {}", entity.contractName(), tenant, exception);
                if (firstFailure == null) {
                    firstFailure = exception;
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        if (firstFailure != null) {
            throw new IllegalStateException("One or more tenant snapshot jobs failed", firstFailure);
        }
    }

    private PollCommand auditCommand(TenantCode tenant, SyncStream stream) {
        return new PollCommand(
                tenant,
                stream,
                pollProperties.batchSize(),
                pollProperties.safetyLag(),
                pollProperties.overlapAuditRows(),
                owner,
                pollProperties.leaseDuration());
    }

    private TimeWindowScanCommand scanCommand(TenantCode tenant, SyncStream stream) {
        return new TimeWindowScanCommand(
                tenant,
                stream,
                pollProperties.batchSize(),
                scanProperties.expiryLeadDays(),
                scanProperties.invoiceLookbackMonths(),
                scanProperties.invoiceGraceDays(),
                owner,
                pollProperties.leaseDuration());
    }

    private String requiredOption(ApplicationArguments arguments, String optionName) {
        List<String> values = arguments.getOptionValues(optionName);
        if (values == null || values.size() != 1 || values.get(0).isBlank()) {
            throw new IllegalArgumentException("--" + optionName + " must be provided exactly once");
        }
        return values.get(0).trim();
    }

    private String optionalOption(ApplicationArguments arguments, String optionName) {
        List<String> values = arguments.getOptionValues(optionName);
        if (values == null) {
            return null;
        }
        if (values.size() != 1 || values.get(0).isBlank()) {
            throw new IllegalArgumentException("--" + optionName + " must be provided at most once and not be blank");
        }
        return values.get(0).trim();
    }

    private UUID parsePublicId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("--sync.public-id must be a UUID", exception);
        }
    }

    private Instant parseInstant(String value, String optionName) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("--" + optionName + " must be an ISO-8601 instant", exception);
        }
    }

    private TenantCode parseTenant(String tenantValue) {
        try {
            return TenantCode.valueOf(tenantValue.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported tenant: " + tenantValue, exception);
        }
    }
}
