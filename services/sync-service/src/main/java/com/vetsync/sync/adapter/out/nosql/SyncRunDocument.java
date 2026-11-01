package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.domain.SyncRunStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "sync_runs")
record SyncRunDocument(
        @Id String id,
        String tenant,
        String scope,
        SyncRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        String errorType) {
}
