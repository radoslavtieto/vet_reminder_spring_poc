package com.vetsync.sync.application;

import java.time.Instant;

public interface ProcessedEventRepository {

    boolean contains(String idempotencyKey);

    void markProcessed(String idempotencyKey, Instant processedAt);
}
