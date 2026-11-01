package com.vetsync.sync.adapter.out.nosql;

import com.vetsync.sync.domain.SnapshotProgressState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "snapshot_progress")
record SnapshotProgressDocument(
        @Id String id,
        String tenant,
        String entity,
        String runId,
        long lastId,
        SnapshotProgressState state) {
}
