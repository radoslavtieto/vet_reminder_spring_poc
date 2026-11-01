package com.vetsync.sync.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.vetsync.sync.adapter.out.nosql.MongoLeaseRepository;
import com.vetsync.sync.adapter.out.nosql.MongoPointerRepository;
import com.vetsync.sync.adapter.out.nosql.MongoProcessedEventRepository;
import com.vetsync.sync.adapter.out.nosql.MongoSnapshotProgressRepository;
import com.vetsync.sync.adapter.out.nosql.MongoSyncRunStatusRepository;
import com.vetsync.sync.application.LeaseRepository;
import com.vetsync.sync.application.PointerRepository;
import com.vetsync.sync.application.ProcessedEventRepository;
import com.vetsync.sync.application.SnapshotProgressRepository;
import com.vetsync.sync.application.SyncRunStatusRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "sync.state.mongo", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MongoStateProperties.class)
public class MongoStateConfiguration {

    @Bean
    MongoClient syncMongoClient(MongoStateProperties properties) {
        return MongoClients.create(properties.uri());
    }

    @Bean
    MongoDatabaseFactory syncMongoDatabaseFactory(MongoClient syncMongoClient, MongoStateProperties properties) {
        return new SimpleMongoClientDatabaseFactory(syncMongoClient, properties.database());
    }

    @Bean
    MongoTemplate syncMongoTemplate(MongoDatabaseFactory syncMongoDatabaseFactory) {
        return new MongoTemplate(syncMongoDatabaseFactory);
    }

    @Bean
    PointerRepository mongoPointerRepository(MongoTemplate syncMongoTemplate) {
        return new MongoPointerRepository(syncMongoTemplate);
    }

    @Bean
    ProcessedEventRepository mongoProcessedEventRepository(
            MongoTemplate syncMongoTemplate) {
        return new MongoProcessedEventRepository(syncMongoTemplate);
    }

    @Bean
    LeaseRepository mongoLeaseRepository(MongoTemplate syncMongoTemplate) {
        return new MongoLeaseRepository(syncMongoTemplate);
    }

    @Bean
    SnapshotProgressRepository mongoSnapshotProgressRepository(MongoTemplate syncMongoTemplate) {
        return new MongoSnapshotProgressRepository(syncMongoTemplate);
    }

    @Bean
    SyncRunStatusRepository mongoSyncRunStatusRepository(MongoTemplate syncMongoTemplate) {
        return new MongoSyncRunStatusRepository(syncMongoTemplate);
    }

    @Bean
    InitializingBean ensureMongoStateIndexes(
            MongoTemplate syncMongoTemplate,
            MongoStateProperties properties) {
        return () -> syncMongoTemplate.indexOps("processed_events")
                    .createIndex(new Index()
                            .on("processedAt", Sort.Direction.ASC)
                            .expire(Duration.ofDays(properties.dedupeRetentionDays()))
                            .named("ttl_processed_events"));
    }
}
