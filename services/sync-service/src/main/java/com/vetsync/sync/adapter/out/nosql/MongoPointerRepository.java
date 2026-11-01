package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.PointerRepository;
import com.vetsync.sync.domain.SyncPointer;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Objects;
import java.util.Optional;

public final class MongoPointerRepository implements PointerRepository {

    private final MongoTemplate mongoTemplate;

    public MongoPointerRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    public Optional<SyncPointer> find(TenantCode tenant, SyncStream stream) {
        String id = pointerId(tenant, stream);
        SyncPointerDocument document = mongoTemplate.findById(id, SyncPointerDocument.class);
        if (document == null) {
            return Optional.empty();
        }
        return Optional.of(new SyncPointer(tenant, stream, document.lastAuditId()));
    }

    @Override
    public void save(SyncPointer pointer) {
        mongoTemplate.save(new SyncPointerDocument(
                pointerId(pointer.tenant(), pointer.stream()),
                pointer.tenant().name(),
                pointer.stream().contractName(),
                pointer.lastAuditId()));
    }

    private String pointerId(TenantCode tenant, SyncStream stream) {
        return tenant.name() + ":" + stream.contractName();
    }
}
