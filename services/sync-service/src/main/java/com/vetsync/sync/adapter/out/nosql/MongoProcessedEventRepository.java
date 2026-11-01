package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.application.ProcessedEventRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.Objects;

public final class MongoProcessedEventRepository implements ProcessedEventRepository {

    private final MongoTemplate mongoTemplate;

    public MongoProcessedEventRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    public boolean contains(String idempotencyKey) {
        return mongoTemplate.exists(
                Query.query(Criteria.where("_id").is(idempotencyKey)),
                ProcessedEventDocument.class);
    }

    @Override
    public void markProcessed(String idempotencyKey, Instant processedAt) {
        Query query = Query.query(Criteria.where("_id").is(idempotencyKey));
        Update update = new Update().setOnInsert("processedAt", processedAt);
        mongoTemplate.upsert(query, update, ProcessedEventDocument.class);
    }
}
