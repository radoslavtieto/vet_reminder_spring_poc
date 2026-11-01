package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.PollStreamUseCase;
import com.vetsync.sync.application.SnapshotSyncUseCase;
import com.vetsync.sync.application.SyncRunStatusRepository;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.application.TimeWindowScanUseCase;
import com.vetsync.sync.config.SyncPollProperties;
import com.vetsync.sync.config.TimeWindowScanProperties;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotMode;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.sync.domain.SnapshotSyncResult;
import com.vetsync.sync.domain.SnapshotSyncStatus;
import com.vetsync.sync.domain.TenantCode;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import com.vetsync.sync.domain.TimeWindowScanResult;
import com.vetsync.sync.domain.TimeWindowScanStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollJobRunnerTests {

    private final PollStreamUseCase poller = mock(PollStreamUseCase.class);
    private final TimeWindowScanUseCase scanner = mock(TimeWindowScanUseCase.class);
    private final SnapshotSyncUseCase snapshotter = mock(SnapshotSyncUseCase.class);
    private final SyncRunTracker runTracker = new SyncRunTracker(
            mock(SyncRunStatusRepository.class), Clock.systemUTC());
    private final SyncPollProperties pollProperties = new SyncPollProperties(
            true, 10, Duration.ofSeconds(5), 2, Duration.ofMinutes(5), 5_000);
    private final TimeWindowScanProperties scanProperties = new TimeWindowScanProperties(30, 6, 7, 3_600_000);
    private final PollJobRunner runner = new PollJobRunner(
            poller, scanner, snapshotter, runTracker, pollProperties, scanProperties);

    @Test
    void runsOnePassForEveryTenantWhenTenantIsAll() {
        when(poller.execute(any(PollCommand.class)))
                .thenReturn(new PollResult(PollStatus.COMPLETED, 0, 0, 0, 0));

        runner.run(new DefaultApplicationArguments(
                "--sync.stream=master-data",
                "--sync.tenant=ALL"));

        ArgumentCaptor<PollCommand> commandCaptor = ArgumentCaptor.forClass(PollCommand.class);
        verify(poller, times(6)).execute(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(command -> command.tenant())
                .containsExactly(TenantCode.values());
        assertThat(commandCaptor.getAllValues())
                .allSatisfy(command -> assertThat(command.stream()).isEqualTo(SyncStream.MASTER_DATA));
    }

    @Test
    void runsOnePassForSelectedTenantAndStream() {
        when(poller.execute(any(PollCommand.class)))
                .thenReturn(new PollResult(PollStatus.COMPLETED, 0, 0, 0, 0));

        runner.run(new DefaultApplicationArguments(
                "--sync.stream=invoices",
                "--sync.tenant=SE"));

        ArgumentCaptor<PollCommand> commandCaptor = ArgumentCaptor.forClass(PollCommand.class);
        verify(poller).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().tenant()).isEqualTo(TenantCode.SE);
        assertThat(commandCaptor.getValue().stream()).isEqualTo(SyncStream.INVOICES);
    }

    @Test
    void routesTimeWindowStreamsToTheScanUseCase() {
        when(scanner.execute(any(TimeWindowScanCommand.class)))
                .thenReturn(new TimeWindowScanResult(TimeWindowScanStatus.COMPLETED, 2, 1, 1));

        runner.run(new DefaultApplicationArguments(
                "--sync.stream=unpaid-invoices",
                "--sync.tenant=SE"));

        ArgumentCaptor<TimeWindowScanCommand> commandCaptor =
                ArgumentCaptor.forClass(TimeWindowScanCommand.class);
        verify(scanner).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().tenant()).isEqualTo(TenantCode.SE);
        assertThat(commandCaptor.getValue().stream()).isEqualTo(SyncStream.UNPAID_INVOICES);
        assertThat(commandCaptor.getValue().invoiceLookbackMonths()).isEqualTo(6);
        assertThat(commandCaptor.getValue().invoiceGraceDays()).isEqualTo(7);
        verify(poller, org.mockito.Mockito.never()).execute(any(PollCommand.class));
    }

    @Test
    void routesInitialSnapshotForSelectedEntityAndTenant() {
        when(snapshotter.execute(any(SnapshotSyncCommand.class)))
                .thenReturn(new SnapshotSyncResult(SnapshotSyncStatus.COMPLETED, 2, 2, 0, 2));

        runner.run(new DefaultApplicationArguments(
                "--sync.operation=initial",
                "--sync.entity=client",
                "--sync.tenant=SW"));

        ArgumentCaptor<SnapshotSyncCommand> commandCaptor = ArgumentCaptor.forClass(SnapshotSyncCommand.class);
        verify(snapshotter).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().tenant()).isEqualTo(TenantCode.SW);
        assertThat(commandCaptor.getValue().entity()).isEqualTo(SnapshotEntity.CLIENT);
        assertThat(commandCaptor.getValue().mode()).isEqualTo(SnapshotMode.INITIAL);
    }

    @Test
    void routesPartialSnapshotByTimeRangeWithRequiredRunId() {
        when(snapshotter.execute(any(SnapshotSyncCommand.class)))
                .thenReturn(new SnapshotSyncResult(SnapshotSyncStatus.COMPLETED, 1, 1, 0, 5));

        runner.run(new DefaultApplicationArguments(
                "--sync.operation=partial",
                "--sync.entity=client",
                "--sync.tenant=SW",
                "--sync.run-id=manual-replay-1",
                "--sync.updated-from=2026-10-01T00:00:00Z",
                "--sync.updated-to=2026-11-01T00:00:00Z"));

        ArgumentCaptor<SnapshotSyncCommand> commandCaptor = ArgumentCaptor.forClass(SnapshotSyncCommand.class);
        verify(snapshotter).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().mode()).isEqualTo(SnapshotMode.PARTIAL);
        assertThat(commandCaptor.getValue().runId()).isEqualTo("manual-replay-1");
        assertThat(commandCaptor.getValue().updatedFromInclusive())
                .isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(commandCaptor.getValue().updatedToExclusive())
                .isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void rejectsUnsupportedJobInputsBeforePolling() {
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(
                "--sync.stream=not-a-stream",
                "--sync.tenant=SW")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sync stream");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(
                "--sync.stream=master-data")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--sync.tenant must be provided");
        verify(poller, org.mockito.Mockito.never()).execute(any(PollCommand.class));
        verify(scanner, org.mockito.Mockito.never()).execute(any(TimeWindowScanCommand.class));
    }
}
