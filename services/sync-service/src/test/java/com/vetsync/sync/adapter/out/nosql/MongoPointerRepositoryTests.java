package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.domain.SyncPointer;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoPointerRepositoryTests {

    private final MongoTemplate mongoTemplate = org.mockito.Mockito.mock(MongoTemplate.class);
    private final MongoPointerRepository repository = new MongoPointerRepository(mongoTemplate);

    @Test
    void loadsPointerUsingTenantAndStreamKey() {
        when(mongoTemplate.findById("SW:master-data", SyncPointerDocument.class))
                .thenReturn(new SyncPointerDocument("SW:master-data", "SW", "master-data", 42));

        assertThat(repository.find(TenantCode.SW, SyncStream.MASTER_DATA))
                .contains(new SyncPointer(TenantCode.SW, SyncStream.MASTER_DATA, 42));
    }

    @Test
    void returnsEmptyWhenPointerDoesNotExist() {
        when(mongoTemplate.findById("SW:master-data", SyncPointerDocument.class)).thenReturn(null);

        assertThat(repository.find(TenantCode.SW, SyncStream.MASTER_DATA)).isEqualTo(Optional.empty());
    }

    @Test
    void savesPointerUsingStableTenantAndStreamKey() {
        SyncPointer pointer = new SyncPointer(TenantCode.SW, SyncStream.MASTER_DATA, 42);

        repository.save(pointer);

        verify(mongoTemplate).save(new SyncPointerDocument("SW:master-data", "SW", "master-data", 42));
    }
}
