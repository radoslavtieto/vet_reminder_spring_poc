package com.vetsync.sync.application;

import com.vetsync.sync.domain.SyncRun;
import com.vetsync.sync.domain.SyncRunStatus;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncRunTrackerTests {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @Test
    void recordsRunStartAndCompletionForSuccessfulOperation() {
        InMemoryRunStatusRepository repository = new InMemoryRunStatusRepository();
        SyncRunTracker tracker = tracker(repository);

        String result = tracker.track(TenantCode.SW, "invoices", () -> "completed");

        assertThat(result).isEqualTo("completed");
        assertThat(repository.runs).singleElement().satisfies(run -> {
            assertThat(run.tenant()).isEqualTo(TenantCode.SW);
            assertThat(run.scope()).isEqualTo("invoices");
            assertThat(run.status()).isEqualTo(SyncRunStatus.COMPLETED);
            assertThat(run.startedAt()).isEqualTo(NOW);
            assertThat(run.finishedAt()).isEqualTo(NOW);
            assertThat(run.errorType()).isNull();
        });
    }

    @Test
    void persistsFailedStatusAndPropagatesOperationFailure() {
        InMemoryRunStatusRepository repository = new InMemoryRunStatusRepository();
        SyncRunTracker tracker = tracker(repository);

        assertThatThrownBy(() -> tracker.track(TenantCode.SE, "visits", () -> {
            throw new IllegalStateException("source unavailable");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("source unavailable");

        assertThat(repository.runs).singleElement().satisfies(run -> {
            assertThat(run.status()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(run.errorType()).isEqualTo("IllegalStateException");
            assertThat(run.finishedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void exposesStatusPersistenceFailureWithoutExecutingOperation() {
        SyncRunTracker tracker = tracker(run -> {
            throw new IllegalStateException("state store unavailable");
        });
        boolean[] executed = {false};

        assertThatThrownBy(() -> tracker.track(TenantCode.SW, "master-data", () -> {
            executed[0] = true;
            return null;
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("state store unavailable");

        assertThat(executed[0]).isFalse();
    }

    @Test
    void doesNotMarkCompletedOperationFailedWhenCompletionStatusCannotBeSaved() {
        List<SyncRun> persisted = new ArrayList<>();
        SyncRunStatusRepository repository = run -> {
            if (run.status() == SyncRunStatus.COMPLETED) {
                throw new IllegalStateException("state store unavailable");
            }
            persisted.add(run);
        };
        SyncRunTracker tracker = tracker(repository);

        assertThatThrownBy(() -> tracker.track(TenantCode.SW, "invoices", () -> "completed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("state store unavailable");

        assertThat(persisted).singleElement()
                .extracting(SyncRun::status)
                .isEqualTo(SyncRunStatus.RUNNING);
    }

    private static SyncRunTracker tracker(SyncRunStatusRepository repository) {
        return new SyncRunTracker(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class InMemoryRunStatusRepository implements SyncRunStatusRepository {

        private final List<SyncRun> runs = new ArrayList<>();

        @Override
        public void save(SyncRun run) {
            runs.removeIf(existing -> existing.id().equals(run.id()));
            runs.add(run);
        }
    }
}
