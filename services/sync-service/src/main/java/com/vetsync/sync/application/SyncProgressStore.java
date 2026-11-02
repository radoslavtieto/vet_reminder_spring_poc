package com.vetsync.sync.application;

public interface SyncProgressStore {
    long lastAuditId(String tenant, String stream);

    void advance(String tenant, String stream, long auditId);
}
