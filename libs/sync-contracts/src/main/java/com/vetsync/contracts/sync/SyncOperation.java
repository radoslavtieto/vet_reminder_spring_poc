package com.vetsync.contracts.sync;

public enum SyncOperation {
    UPSERT,
    DELETE,
    EXPIRY_DUE,
    OVERDUE
}
