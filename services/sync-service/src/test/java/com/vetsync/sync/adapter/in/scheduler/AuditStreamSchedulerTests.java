package com.vetsync.sync.adapter.in.scheduler;

import com.vetsync.sync.application.PollStreamUseCase;
import com.vetsync.sync.application.SyncRunStatusRepository;
import com.vetsync.sync.application.SyncRunTracker;
import com.vetsync.sync.config.SyncPollProperties;
import com.vetsync.sync.domain.PollCommand;
import com.vetsync.sync.domain.PollResult;
import com.vetsync.sync.domain.PollStatus;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Clock;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditStreamSchedulerTests {

    private final PollStreamUseCase poller = mock(PollStreamUseCase.class);
    private final SyncPollProperties properties = new SyncPollProperties(
            true, 10, Duration.ofSeconds(5), 2, Duration.ofMinutes(5), 5_000);
    private final SyncRunTracker runTracker = new SyncRunTracker(
            mock(SyncRunStatusRepository.class), Clock.systemUTC());
    private final AuditStreamScheduler scheduler = new AuditStreamScheduler(poller, runTracker, properties);

    @Test
    void pollsEveryAuditStreamForEveryTenant() {
        when(poller.execute(any(PollCommand.class)))
                .thenReturn(new PollResult(PollStatus.COMPLETED, 0, 0, 0, 0));

        scheduler.pollAuditStreams();

        ArgumentCaptor<PollCommand> commandCaptor = ArgumentCaptor.forClass(PollCommand.class);
        verify(poller, times(TenantCode.values().length * 4)).execute(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(command -> command.tenant().name() + ":" + command.stream().contractName())
                .containsExactlyInAnyOrderElementsOf(Set.of(
                        "SW:master-data", "SW:visits", "SW:prescriptions", "SW:invoices",
                        "SE:master-data", "SE:visits", "SE:prescriptions", "SE:invoices",
                        "SC:master-data", "SC:visits", "SC:prescriptions", "SC:invoices",
                        "NE:master-data", "NE:visits", "NE:prescriptions", "NE:invoices",
                        "NW:master-data", "NW:visits", "NW:prescriptions", "NW:invoices",
                        "NC:master-data", "NC:visits", "NC:prescriptions", "NC:invoices"));
    }

    @Test
    void continuesPollingOtherTenantStreamsAfterOneFailure() {
        doThrow(new IllegalStateException("source unavailable"))
                .doReturn(new PollResult(PollStatus.COMPLETED, 0, 0, 0, 0))
                .when(poller).execute(any(PollCommand.class));

        scheduler.pollAuditStreams();

        verify(poller, times(TenantCode.values().length * 4)).execute(any(PollCommand.class));
    }

    @Test
    void pollsAllChangeLogStreamKinds() {
        assertThat(SyncStream.values())
                .filteredOn(SyncStream::isAuditStream)
                .containsExactly(
                        SyncStream.MASTER_DATA,
                        SyncStream.VISITS,
                        SyncStream.PRESCRIPTIONS,
                        SyncStream.INVOICES);
    }
}
