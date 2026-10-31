package com.vetsync.contracts;

public enum SyncOperation {
    UPSERT,
    DELETE,
    EXPIRY_DUE,
    OVERDUE
}
