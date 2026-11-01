package com.vetsync.sync.adapter.out.nosql;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sync_pointers")
record SyncPointerDocument(
        @Id String id,
        String tenant,
        String stream,
        long lastAuditId) {
}
