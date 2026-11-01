package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.domain.SyncRun;
import com.vetsync.sync.domain.SyncRunStatus;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MongoSyncRunStatusRepositoryTests {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoSyncRunStatusRepository repository = new MongoSyncRunStatusRepository(mongoTemplate);

    @Test
    void persistsRunStatusAndFailureTypeUsingRunId() {
        SyncRun run = new SyncRun(
                UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c"),
                TenantCode.SW,
                "invoices",
                SyncRunStatus.FAILED,
                Instant.parse("2026-10-31T10:00:00Z"),
                Instant.parse("2026-10-31T10:00:01Z"),
                "IllegalStateException");

        repository.save(run);

        verify(mongoTemplate).save(new SyncRunDocument(
                run.id().toString(), "SW", "invoices", SyncRunStatus.FAILED,
                run.startedAt(), run.finishedAt(), "IllegalStateException"));
    }
}
