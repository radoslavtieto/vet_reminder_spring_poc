package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.PollStreamUseCase;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.config.SyncPollProperties;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.UUID;

public final class AuditStreamScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditStreamScheduler.class);

    private final PollStreamUseCase pollStreamUseCase;
    private final SyncRunTracker syncRunTracker;
    private final SyncPollProperties properties;
    private final String owner = UUID.randomUUID().toString();

    public AuditStreamScheduler(
            PollStreamUseCase pollStreamUseCase,
            SyncRunTracker syncRunTracker,
            SyncPollProperties properties) {
        this.pollStreamUseCase = Objects.requireNonNull(pollStreamUseCase, "pollStreamUseCase");
        this.syncRunTracker = Objects.requireNonNull(syncRunTracker, "syncRunTracker");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Scheduled(fixedDelayString = "${sync.poll.audit-fixed-delay-ms:5000}")
    public void pollAuditStreams() {
        for (SyncStream stream : SyncStream.values()) {
            if (!stream.isAuditStream()) {
                continue;
            }
            for (TenantCode tenant : TenantCode.values()) {
                try {
                    PollResult result = syncRunTracker.track(
                            tenant,
                            stream.contractName(),
                            () -> pollStreamUseCase.execute(new PollCommand(
                                    tenant,
                                    stream,
                                    properties.batchSize(),
                                    properties.safetyLag(),
                                    properties.overlapAuditRows(),
                                    owner,
                                    properties.leaseDuration())));
                    if (result.status() == PollStatus.SKIPPED_LEASED) {
                        LOGGER.debug("Skipping {} poll for {} because its lease is held",
                                stream.contractName(), tenant);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.error("{} poll failed for tenant {}", stream.contractName(), tenant, exception);
                }
            }
        }
    }
}
