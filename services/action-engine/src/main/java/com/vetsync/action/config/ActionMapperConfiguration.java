package com.vetsync.action.config;

import com.vetsync.action.application.SyncEnvelopeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionMapperConfiguration {
    @Bean
    SyncEnvelopeMapper syncEnvelopeMapper() {
        return new SyncEnvelopeMapper();
    }
}
