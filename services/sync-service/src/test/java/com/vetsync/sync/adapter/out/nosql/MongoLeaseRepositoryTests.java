package com.vetsync.sync.adapter.out.nosql;

import com.mongodb.client.result.UpdateResult;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoLeaseRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoLeaseRepository repository = new MongoLeaseRepository(mongoTemplate);

    @Test
    void atomicallyAcquiresExpiredOrUnownedLease() {
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(SyncLeaseDocument.class)))
                .thenReturn(new SyncLeaseDocument("SW:master-data", "worker-1", NOW.plusSeconds(60)));

        assertThat(repository.tryAcquire(
                TenantCode.SW, "master-data", "worker-1", NOW, NOW.plusSeconds(60))).isTrue();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findAndModify(
                queryCaptor.capture(),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(SyncLeaseDocument.class));
        assertThat(queryCaptor.getValue().getQueryObject()).containsKey("_id");
    }

    @Test
    void treatsDuplicateInsertAsLeaseHeldByAnotherWorker() {
        doThrow(new DuplicateKeyException("lease already exists"))
                .when(mongoTemplate).findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(SyncLeaseDocument.class));

        assertThat(repository.tryAcquire(
                TenantCode.SW, "master-data", "worker-2", NOW, NOW.plusSeconds(60))).isFalse();
    }

    @Test
    void renewsOnlyLeaseOwnedByCurrentWorkerAndReleasesByOwner() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(SyncLeaseDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        assertThat(repository.renew(
                TenantCode.SW, "master-data", "worker-1", NOW, NOW.plusSeconds(60))).isTrue();

        repository.release(TenantCode.SW, "master-data", "worker-1");

        verify(mongoTemplate).remove(any(Query.class), eq(SyncLeaseDocument.class));
    }
}
