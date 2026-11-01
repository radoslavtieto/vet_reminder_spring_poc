package com.vetsync.sync.application;

import com.vetsync.sync.domain.SnapshotRecord;
import com.vetsync.sync.domain.SnapshotSyncCommand;

import java.util.List;

public interface SnapshotSource {

    List<SnapshotRecord> findSnapshotPage(SnapshotSyncCommand command, long afterSourceId);
}
