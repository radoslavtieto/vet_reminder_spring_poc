package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

public final class RetryingEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingEventPublisher.class);

    private final EventPublisher delegate;
    private final EventQuarantine quarantine;
    private final int maxAttempts;
    private final Duration retryDelay;
    private final RetryDelay delay;

    public RetryingEventPublisher(
            EventPublisher delegate,
            EventQuarantine quarantine,
            int maxAttempts,
            Duration retryDelay,
            RetryDelay delay) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.quarantine = Objects.requireNonNull(quarantine, "quarantine");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay");
        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must not be negative");
        }
        this.delay = Objects.requireNonNull(delay, "delay");
    }

    public static RetryDelay threadSleepDelay() {
        return duration -> {
            if (duration.isZero()) {
                return;
            }
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting to retry event publishing", exception);
            }
        };
    }

    @Override
    public void publish(SyncEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                delegate.publish(envelope);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt < maxAttempts) {
                    delay.pause(retryDelay);
                }
            }
        }

        String failureReason = lastFailure.getClass().getSimpleName() + ": " + lastFailure.getMessage();
        try {
            quarantine.quarantine(envelope, failureReason);
            LOGGER.error("Event {} failed to publish after {} attempts and was quarantined",
                    envelope.eventId(), maxAttempts, lastFailure);
        } catch (RuntimeException quarantineFailure) {
            IllegalStateException combinedFailure = new IllegalStateException(
                    "Event publish and quarantine both failed for " + envelope.eventId(), lastFailure);
            combinedFailure.addSuppressed(quarantineFailure);
            throw combinedFailure;
        }
    }

    @FunctionalInterface
    public interface RetryDelay {

        void pause(Duration duration);
    }
}
