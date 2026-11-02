package com.vetsync.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.sync.application.AuditChangeSource;
import com.vetsync.sync.application.PollMasterDataUseCase;
import com.vetsync.sync.application.SyncEventPublisher;
import com.vetsync.sync.application.SyncProgressStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SyncUseCaseConfiguration {
    @Bean
    Clock syncClock() {
        return Clock.systemUTC();
    }

    @Bean
    PollMasterDataUseCase pollMasterDataUseCase(
            AuditChangeSource source,
            SyncProgressStore progress,
            SyncEventPublisher publisher,
            ObjectMapper objectMapper,
            Clock syncClock,
            @Value("${sync.master-data.tenants:SW,SE,SC,NE,NW,NC}") String tenants,
            @Value("${sync.master-data.safety-lag:PT5S}") Duration safetyLag,
            @Value("${sync.master-data.overlap:PT10S}") Duration overlap,
            @Value("${sync.master-data.batch-size:100}") int batchSize
    ) {
        List<String> tenantCodes = Arrays.stream(tenants.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
        return new PollMasterDataUseCase(source, progress, publisher, objectMapper, syncClock,
                tenantCodes, safetyLag, overlap, batchSize);
    }
}
