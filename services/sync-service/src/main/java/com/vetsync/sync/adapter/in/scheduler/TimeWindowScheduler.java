package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.TimeWindowScanUseCase;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.config.SyncPollProperties;
import com.vetsync.sync.config.TimeWindowScanProperties;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import com.vetsync.sync.domain.TimeWindowScanResult;
import com.vetsync.sync.domain.TimeWindowScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.UUID;

public final class TimeWindowScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeWindowScheduler.class);

    private final TimeWindowScanUseCase timeWindowScanUseCase;
    private final SyncRunTracker syncRunTracker;
    private final SyncPollProperties pollProperties;
    private final TimeWindowScanProperties scanProperties;
    private final String owner = UUID.randomUUID().toString();

    public TimeWindowScheduler(
            TimeWindowScanUseCase timeWindowScanUseCase,
            SyncRunTracker syncRunTracker,
            SyncPollProperties pollProperties,
            TimeWindowScanProperties scanProperties) {
        this.timeWindowScanUseCase = Objects.requireNonNull(timeWindowScanUseCase, "timeWindowScanUseCase");
        this.syncRunTracker = Objects.requireNonNull(syncRunTracker, "syncRunTracker");
        this.pollProperties = Objects.requireNonNull(pollProperties, "pollProperties");
        this.scanProperties = Objects.requireNonNull(scanProperties, "scanProperties");
    }

    @Scheduled(fixedDelayString = "${sync.scan.fixed-delay-ms:3600000}")
    public void scanTimeWindows() {
        for (TenantCode tenant : TenantCode.values()) {
            scan(tenant, SyncStream.PRESCRIPTION_EXPIRY);
            scan(tenant, SyncStream.UNPAID_INVOICES);
        }
    }

    private void scan(TenantCode tenant, SyncStream stream) {
        try {
            TimeWindowScanResult result = syncRunTracker.track(
                    tenant,
                    stream.contractName(),
                    () -> timeWindowScanUseCase.execute(new TimeWindowScanCommand(
                            tenant,
                            stream,
                            pollProperties.batchSize(),
                            scanProperties.expiryLeadDays(),
                            scanProperties.invoiceLookbackMonths(),
                            scanProperties.invoiceGraceDays(),
                            owner,
                            pollProperties.leaseDuration())));
            if (result.status() == TimeWindowScanStatus.SKIPPED_LEASED) {
                LOGGER.debug("Skipping time-window scan {} for {} because its lease is held",
                        stream.contractName(), tenant);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Time-window scan {} failed for tenant {}", stream.contractName(), tenant, exception);
        }
    }
}
