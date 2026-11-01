package com.vetsync.sync.adapter.out.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
record ProcessedEventDocument(
        @Id String idempotencyKey,
        Instant processedAt) {
}
