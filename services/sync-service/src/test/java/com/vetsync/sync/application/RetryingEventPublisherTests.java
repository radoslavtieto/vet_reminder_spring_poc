package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.contracts.sync.SyncOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryingEventPublisherTests {

    private static final SyncEnvelope ENVELOPE = new SyncEnvelope(
            1,
            UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            "SW",
            "master-data",
            "CLIENT",
            UUID.fromString("2d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            SyncOperation.UPSERT,
            7,
            Instant.parse("2026-10-31T10:00:00Z"),
            Instant.parse("2026-10-31T10:00:01Z"),
            Map.of("name", "Synthetic"));

    @Test
    void retriesTransientSendAndDoesNotQuarantineAfterSuccess() {
        List<Duration> delays = new ArrayList<>();
        List<SyncEnvelope> quarantined = new ArrayList<>();
        int[] attempts = {0};
        RetryingEventPublisher publisher = publisher(
                envelope -> {
                    if (++attempts[0] < 3) {
                        throw new IllegalStateException("temporary failure");
                    }
                },
                (envelope, reason) -> quarantined.add(envelope),
                delays::add);

        publisher.publish(ENVELOPE);

        assertThat(attempts[0]).isEqualTo(3);
        assertThat(delays).containsExactly(Duration.ofMillis(10), Duration.ofMillis(10));
        assertThat(quarantined).isEmpty();
    }

    @Test
    void quarantinesAfterAllSendAttemptsFail() {
        List<String> reasons = new ArrayList<>();
        List<SyncEnvelope> quarantined = new ArrayList<>();
        int[] attempts = {0};
        RetryingEventPublisher publisher = publisher(
                envelope -> {
                    attempts[0]++;
                    throw new IllegalArgumentException("permanent send failure");
                },
                (envelope, reason) -> {
                    quarantined.add(envelope);
                    reasons.add(reason);
                },
                delay -> { });

        publisher.publish(ENVELOPE);

        assertThat(attempts[0]).isEqualTo(3);
        assertThat(quarantined).containsExactly(ENVELOPE);
        assertThat(reasons).singleElement().asString().contains("IllegalArgumentException");
    }

    @Test
    void propagatesOriginalFailureWhenQuarantineAlsoFails() {
        RetryingEventPublisher publisher = publisher(
                envelope -> {
                    throw new IllegalStateException("send failed");
                },
                (envelope, reason) -> {
                    throw new IllegalStateException("quarantine unavailable");
                },
                delay -> { });

        assertThatThrownBy(() -> publisher.publish(ENVELOPE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("publish and quarantine both failed")
                .hasRootCauseMessage("send failed")
                .satisfies(exception ->
                        assertThat(exception.getSuppressed()).anySatisfy(
                                suppressed -> assertThat(suppressed).hasMessage("quarantine unavailable")));
    }

    private static RetryingEventPublisher publisher(
            EventPublisher delegate,
            EventQuarantine quarantine,
            RetryingEventPublisher.RetryDelay delay) {
        return new RetryingEventPublisher(delegate, quarantine, 3, Duration.ofMillis(10), delay);
    }
}
