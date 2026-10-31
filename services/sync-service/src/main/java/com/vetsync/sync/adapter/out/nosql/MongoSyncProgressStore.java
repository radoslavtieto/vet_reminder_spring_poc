package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.SyncProgressStore;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MongoSyncProgressStore implements SyncProgressStore {
    private static final String COLLECTION = "sync_pointers";

    private final MongoTemplate mongoTemplate;

    public MongoSyncProgressStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long lastAuditId(String tenant, String stream) {
        Pointer pointer = mongoTemplate.findById(pointerId(tenant, stream), Pointer.class, COLLECTION);
        return pointer == null ? 0L : pointer.lastAuditId;
    }

    @Override
    public void advance(String tenant, String stream, long auditId) {
        Query query = Query.query(Criteria.where("_id").is(pointerId(tenant, stream)));
        Update update = new Update()
                .setOnInsert("tenant", tenant)
                .setOnInsert("stream", stream)
                .max("lastAuditId", auditId);
        mongoTemplate.upsert(query, update, COLLECTION);
    }

    private String pointerId(String tenant, String stream) {
        return tenant + ":" + stream;
    }

    private static final class Pointer {
        private String id;
        private long lastAuditId;
    }
}
