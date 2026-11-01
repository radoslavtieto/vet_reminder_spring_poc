package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.SnapshotProgressRepository;
import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotProgress;
import com.vetsync.sync.domain.SnapshotProgressState;
import com.vetsync.sync.domain.TenantCode;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Objects;
import java.util.Optional;

public final class MongoSnapshotProgressRepository implements SnapshotProgressRepository {

    private final MongoTemplate mongoTemplate;

    public MongoSnapshotProgressRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    public Optional<SnapshotProgress> find(TenantCode tenant, SnapshotEntity entity, String runId) {
        SnapshotProgressDocument document =
                mongoTemplate.findById(progressId(tenant, entity, runId), SnapshotProgressDocument.class);
        if (document == null) {
            return Optional.empty();
        }
        return Optional.of(new SnapshotProgress(
                TenantCode.valueOf(document.tenant()),
                SnapshotEntity.fromContractName(document.entity()),
                document.runId(),
                document.lastId(),
                document.state()));
    }

    @Override
    public void save(SnapshotProgress progress) {
        mongoTemplate.save(new SnapshotProgressDocument(
                progressId(progress.tenant(), progress.entity(), progress.runId()),
                progress.tenant().name(),
                progress.entity().contractName(),
                progress.runId(),
                progress.lastId(),
                progress.state()));
    }

    private String progressId(TenantCode tenant, SnapshotEntity entity, String runId) {
        return tenant.name() + ":" + entity.contractName() + ":" + runId;
    }
}
