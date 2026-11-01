package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.SyncRunStatusRepository;
import com.vetsync.sync.domain.SyncRun;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Objects;

public final class MongoSyncRunStatusRepository implements SyncRunStatusRepository {

    private final MongoTemplate mongoTemplate;

    public MongoSyncRunStatusRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    public void save(SyncRun run) {
        mongoTemplate.save(new SyncRunDocument(
                run.id().toString(),
                run.tenant().name(),
                run.scope(),
                run.status(),
                run.startedAt(),
                run.finishedAt(),
                run.errorType()));
    }
}
