package com.vetsync.sync.config;

import com.vetsync.sync.adapter.in.scheduler.AuditStreamScheduler;
import com.vetsync.sync.adapter.in.scheduler.PollJobRunner;
import com.vetsync.sync.adapter.in.scheduler.TimeWindowScheduler;
import com.vetsync.sync.application.AuditSource;
import com.vetsync.sync.application.EventPublisher;
import com.vetsync.sync.application.LeaseRepository;
import com.vetsync.sync.application.PollStreamUseCase;
import com.vetsync.sync.application.PointerRepository;
import com.vetsync.sync.application.ProcessedEventRepository;
import com.vetsync.sync.application.SnapshotProgressRepository;
import com.vetsync.sync.application.SnapshotSource;
import com.vetsync.sync.application.SnapshotSyncUseCase;
import com.vetsync.sync.application.SyncRunStatusRepository;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.application.TimeWindowScanUseCase;
import com.vetsync.sync.application.TimeWindowSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SyncPollProperties.class, TimeWindowScanProperties.class})
public class PollRuntimeConfiguration {

    @Bean
    Clock syncClock(@Value("${sync.business-zone:Europe/Sofia}") String businessZone) {
        return Clock.system(ZoneId.of(businessZone));
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
    PollStreamUseCase pollStreamUseCase(
            AuditSource auditSource,
            PointerRepository pointerRepository,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock syncClock) {
        return new PollStreamUseCase(
                auditSource, pointerRepository, processedEventRepository, leaseRepository, eventPublisher, syncClock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
    TimeWindowScanUseCase timeWindowScanUseCase(
            TimeWindowSource timeWindowSource,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock syncClock) {
        return new TimeWindowScanUseCase(
                timeWindowSource, processedEventRepository, leaseRepository, eventPublisher, syncClock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
    SnapshotSyncUseCase snapshotSyncUseCase(
            SnapshotSource snapshotSource,
            SnapshotProgressRepository progressRepository,
            ProcessedEventRepository processedEventRepository,
            LeaseRepository leaseRepository,
            EventPublisher eventPublisher,
            Clock syncClock) {
        return new SnapshotSyncUseCase(
                snapshotSource, progressRepository, processedEventRepository, leaseRepository, eventPublisher, syncClock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
    SyncRunTracker syncRunTracker(SyncRunStatusRepository runStatusRepository, Clock syncClock) {
        return new SyncRunTracker(runStatusRepository, syncClock);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "sync.mode", havingValue = "worker", matchIfMissing = true)
    static class WorkerModeConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
        AuditStreamScheduler auditStreamScheduler(
                PollStreamUseCase pollStreamUseCase,
                SyncRunTracker syncRunTracker,
                SyncPollProperties properties) {
            return new AuditStreamScheduler(pollStreamUseCase, syncRunTracker, properties);
        }

        @Bean
        @ConditionalOnProperty(prefix = "sync.poll", name = "enabled", havingValue = "true")
        TimeWindowScheduler timeWindowScheduler(
                TimeWindowScanUseCase timeWindowScanUseCase,
                SyncRunTracker syncRunTracker,
                SyncPollProperties pollProperties,
                TimeWindowScanProperties scanProperties) {
            return new TimeWindowScheduler(timeWindowScanUseCase, syncRunTracker, pollProperties, scanProperties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "sync.mode", havingValue = "job")
    static class JobModeConfiguration {

        @Bean
        PollJobRunner pollJobRunner(
                PollStreamUseCase pollStreamUseCase,
                TimeWindowScanUseCase timeWindowScanUseCase,
                SnapshotSyncUseCase snapshotSyncUseCase,
                SyncRunTracker syncRunTracker,
                SyncPollProperties pollProperties,
                TimeWindowScanProperties scanProperties) {
            return new PollJobRunner(
                    pollStreamUseCase,
                    timeWindowScanUseCase,
                    snapshotSyncUseCase,
                    syncRunTracker,
                    pollProperties,
                    scanProperties);
        }
    }
}
