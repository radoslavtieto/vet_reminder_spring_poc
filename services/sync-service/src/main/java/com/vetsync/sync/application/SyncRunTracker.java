package com.vetsync.sync.application;

import com.vetsync.sync.domain.SyncRun;
import com.vetsync.sync.domain.SyncRunStatus;
import com.vetsync.sync.domain.TenantCode;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class SyncRunTracker {

    private final SyncRunStatusRepository repository;
    private final Clock clock;

    public SyncRunTracker(SyncRunStatusRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public <T> T track(TenantCode tenant, String scope, Supplier<T> operation) {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(operation, "operation");
        UUID id = UUID.randomUUID();
        SyncRun started = new SyncRun(
                id, tenant, scope, SyncRunStatus.RUNNING, clock.instant(), null, null);
        repository.save(started);
        T result;
        try {
            result = operation.get();
        } catch (RuntimeException exception) {
            try {
                repository.save(new SyncRun(
                        id,
                        tenant,
                        scope,
                        SyncRunStatus.FAILED,
                        started.startedAt(),
                        clock.instant(),
                        exception.getClass().getSimpleName()));
            } catch (RuntimeException statusFailure) {
                exception.addSuppressed(statusFailure);
            }
            throw exception;
        }
        repository.save(new SyncRun(
                id,
                tenant,
                scope,
                SyncRunStatus.COMPLETED,
                started.startedAt(),
                clock.instant(),
                null));
        return result;
    }
}
