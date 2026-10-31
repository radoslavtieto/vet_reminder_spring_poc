package com.vetsync.action.application;

import com.vetsync.action.domain.ActionRecord;

import java.util.UUID;

public interface ActionDataStore {
    boolean claimEvent(UUID eventId, String tenant);

    void upsert(ActionRecord record);
}
