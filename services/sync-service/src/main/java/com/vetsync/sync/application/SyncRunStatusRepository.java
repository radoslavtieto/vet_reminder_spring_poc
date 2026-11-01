package com.vetsync.sync.application;

import com.vetsync.sync.domain.SyncRun;

public interface SyncRunStatusRepository {

    void save(SyncRun run);
}
