package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.LeaseRepository;
import com.vetsync.sync.domain.TenantCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.Objects;

public final class MongoLeaseRepository implements LeaseRepository {

    private final MongoTemplate mongoTemplate;

    public MongoLeaseRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    public boolean tryAcquire(
            TenantCode tenant,
            String scope,
            String owner,
            Instant now,
            Instant expiresAt) {
        String id = leaseId(tenant, scope);
        Query query = Query.query(Criteria.where("_id").is(id).and("expiresAt").lte(now));
        Update update = new Update()
                .set("owner", owner)
                .set("expiresAt", expiresAt);
        try {
            SyncLeaseDocument acquired = mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    SyncLeaseDocument.class);
            return acquired != null;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public boolean renew(
            TenantCode tenant,
            String scope,
            String owner,
            Instant now,
            Instant expiresAt) {
        Query query = Query.query(Criteria.where("_id")
                .is(leaseId(tenant, scope))
                .and("owner").is(owner)
                .and("expiresAt").gt(now));
        Update update = new Update().set("expiresAt", expiresAt);
        return mongoTemplate.updateFirst(query, update, SyncLeaseDocument.class).getMatchedCount() == 1;
    }

    @Override
    public void release(TenantCode tenant, String scope, String owner) {
        Query query = Query.query(Criteria.where("_id")
                .is(leaseId(tenant, scope))
                .and("owner").is(owner));
        mongoTemplate.remove(query, SyncLeaseDocument.class);
    }

    private String leaseId(TenantCode tenant, String scope) {
        return tenant.name() + ":" + scope;
    }
}
