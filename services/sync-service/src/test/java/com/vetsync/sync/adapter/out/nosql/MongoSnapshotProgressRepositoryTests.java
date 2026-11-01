package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotProgress;
import com.vetsync.sync.domain.SnapshotProgressState;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoSnapshotProgressRepositoryTests {

    private final MongoTemplate mongoTemplate = org.mockito.Mockito.mock(MongoTemplate.class);
    private final MongoSnapshotProgressRepository repository = new MongoSnapshotProgressRepository(mongoTemplate);

    @Test
    void loadsProgressUsingTenantEntityAndRunIdKey() {
        when(mongoTemplate.findById("SW:client:initial", SnapshotProgressDocument.class))
                .thenReturn(new SnapshotProgressDocument(
                        "SW:client:initial", "SW", "client", "initial", 42, SnapshotProgressState.IN_PROGRESS));

        assertThat(repository.find(TenantCode.SW, SnapshotEntity.CLIENT, "initial"))
                .contains(new SnapshotProgress(
                        TenantCode.SW, SnapshotEntity.CLIENT, "initial", 42, SnapshotProgressState.IN_PROGRESS));
    }

    @Test
    void returnsEmptyWhenSnapshotProgressDoesNotExist() {
        when(mongoTemplate.findById("SW:client:initial", SnapshotProgressDocument.class)).thenReturn(null);

        assertThat(repository.find(TenantCode.SW, SnapshotEntity.CLIENT, "initial"))
                .isEqualTo(Optional.empty());
    }

    @Test
    void savesProgressUsingStableCompoundKey() {
        SnapshotProgress progress = new SnapshotProgress(
                TenantCode.SW, SnapshotEntity.CLIENT, "partial:replay-1", 42, SnapshotProgressState.DONE);

        repository.save(progress);

        verify(mongoTemplate).save(new SnapshotProgressDocument(
                "SW:client:partial:replay-1", "SW", "client", "partial:replay-1",
                42, SnapshotProgressState.DONE));
    }
}
