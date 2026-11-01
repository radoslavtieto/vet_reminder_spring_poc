package com.vetsync.sync.application;

import com.vetsync.contracts.sync.SyncOperation;
import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.domain.TimeWindowRecord;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import com.vetsync.sync.domain.TimeWindowScanResult;
import com.vetsync.sync.domain.TimeWindowScanStatus;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeWindowScanUseCaseTests {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Sofia");
    private static final Instant NOW = Instant.parse("2026-10-30T22:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Test
    void publishesExpiryDueWithDateScopedIdempotency() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(1, "PRESCRIPTION")));
        List<SyncEnvelope> published = new ArrayList<>();

        TimeWindowScanResult result = useCase(ports, published::add).execute(command(SyncStream.PRESCRIPTION_EXPIRY));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(published).singleElement().satisfies(envelope -> {
            assertThat(envelope.operation()).isEqualTo(SyncOperation.EXPIRY_DUE);
            assertThat(envelope.stream()).isEqualTo("prescription-expiry");
            assertThat(envelope.occurredAt()).isEqualTo(
                    LocalDate.of(2026, 10, 30).atStartOfDay(BUSINESS_ZONE).toInstant());
        });
        assertThat(ports.processedKeys).contains("SW:PRESCRIPTION:"
                + record(1, "PRESCRIPTION").publicId()
                + ":7:prescription-expiry:2026-10-31");
        assertThat(ports.requestToday).isEqualTo(TODAY);
        assertThat(ports.request.expiryLeadDays()).isEqualTo(30);
    }

    @Test
    void usesDueDateLookbackAndGracePolicyForUnpaidInvoices() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(4, "INVOICE")));
        List<SyncEnvelope> published = new ArrayList<>();

        TimeWindowScanResult result = useCase(ports, published::add).execute(command(SyncStream.UNPAID_INVOICES));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(ports.request.invoiceLookbackMonths()).isEqualTo(6);
        assertThat(ports.request.invoiceGraceDays()).isEqualTo(7);
        assertThat(published).singleElement()
                .extracting(SyncEnvelope::operation)
                .isEqualTo(SyncOperation.OVERDUE);
    }

    @Test
    void skipsARecordAlreadyProcessedForTheSameScanDate() {
        TimeWindowRecord record = record(1, "PRESCRIPTION");
        InMemoryPorts ports = new InMemoryPorts(List.of(record));
        ports.processedKeys.add(idempotencyKey(record, SyncStream.PRESCRIPTION_EXPIRY));

        TimeWindowScanResult result = useCase(ports, envelope -> {
            throw new AssertionError("duplicate event must not be published");
        }).execute(command(SyncStream.PRESCRIPTION_EXPIRY));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.duplicateCount()).isEqualTo(1);
    }

    @Test
    void releasesLeaseAndLeavesMarkerMissingWhenPublishFails() {
        InMemoryPorts ports = new InMemoryPorts(List.of(record(1, "PRESCRIPTION")));

        assertThatThrownBy(() -> useCase(ports, envelope -> {
            throw new IllegalStateException("broker unavailable");
        }).execute(command(SyncStream.PRESCRIPTION_EXPIRY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.operations).contains("release-lease");
    }

    @Test
    void followsKeysetPagesUntilTheTimeWindowIsExhausted() {
        InMemoryPorts ports = new InMemoryPorts(List.of(
                record(1, "PRESCRIPTION"),
                record(2, "PRESCRIPTION"),
                record(3, "PRESCRIPTION")));
        List<SyncEnvelope> published = new ArrayList<>();

        TimeWindowScanResult result = useCase(ports, published::add).execute(
                new TimeWindowScanCommand(
                        TenantCode.SW, SyncStream.PRESCRIPTION_EXPIRY, 2, 30, 6, 7,
                        "worker-1", Duration.ofMinutes(5)));

        assertThat(result.readCount()).isEqualTo(3);
        assertThat(result.publishedCount()).isEqualTo(3);
        assertThat(ports.requestedCursors).containsExactly(0L, 2L);
    }

    @Test
    void rejectsUnorderedScanPagesBeforePublishingAnyRecord() {
        InMemoryPorts ports = new InMemoryPorts(List.of(
                record(2, "PRESCRIPTION"),
                record(1, "PRESCRIPTION")));

        assertThatThrownBy(() -> useCase(ports, envelope -> {
            throw new AssertionError("invalid page must not be published");
        }).execute(command(SyncStream.PRESCRIPTION_EXPIRY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("strictly increasing sourceId");
        assertThat(ports.processedKeys).isEmpty();
        assertThat(ports.operations).contains("release-lease");
    }

    private static TimeWindowScanUseCase useCase(InMemoryPorts ports, EventPublisher publisher) {
        return new TimeWindowScanUseCase(
                ports,
                ports,
                ports,
                publisher,
                Clock.fixed(NOW, BUSINESS_ZONE));
    }

    private static TimeWindowScanCommand command(SyncStream stream) {
        return new TimeWindowScanCommand(
                TenantCode.SW,
                stream,
                2,
                30,
                6,
                7,
                "worker-1",
                Duration.ofMinutes(5));
    }

    private static TimeWindowRecord record(long sourceId, String entityType) {
        return new TimeWindowRecord(
                sourceId,
                entityType,
                UUID.nameUUIDFromBytes(("synthetic-record-" + sourceId).getBytes(StandardCharsets.UTF_8)),
                7,
                LocalDate.of(2026, 10, 30).atStartOfDay(BUSINESS_ZONE).toInstant(),
                Map.of("id", sourceId, "tenant_id", "SW"));
    }

    private static String idempotencyKey(TimeWindowRecord record, SyncStream stream) {
        return TenantCode.SW.name() + ":" + record.entityType() + ":" + record.publicId() + ":"
                + record.sourceVersion() + ":" + stream.contractName() + ":" + TODAY;
    }

    private static final class InMemoryPorts
            implements TimeWindowSource, ProcessedEventRepository, LeaseRepository {

        private final List<TimeWindowRecord> records;
        private final Set<String> processedKeys = new HashSet<>();
        private final List<String> operations = new ArrayList<>();
        private final List<Long> requestedCursors = new ArrayList<>();
        private TimeWindowScanCommand request;
        private LocalDate requestToday;

        private InMemoryPorts(List<TimeWindowRecord> records) {
            this.records = records;
        }

        @Override
        public List<TimeWindowRecord> findEligible(
                TimeWindowScanCommand command,
                LocalDate today,
                long afterSourceId) {
            request = command;
            requestToday = today;
            requestedCursors.add(afterSourceId);
            return records.stream()
                    .filter(record -> record.sourceId() > afterSourceId)
                    .limit(command.batchSize())
                    .toList();
        }

        @Override
        public boolean contains(String idempotencyKey) {
            return processedKeys.contains(idempotencyKey);
        }

        @Override
        public void markProcessed(String idempotencyKey, Instant processedAt) {
            processedKeys.add(idempotencyKey);
        }

        @Override
        public boolean tryAcquire(
                TenantCode tenant,
                String scope,
                String owner,
                Instant now,
                Instant expiresAt) {
            operations.add("acquire-lease");
            return true;
        }

        @Override
        public boolean renew(
                TenantCode tenant,
                String scope,
                String owner,
                Instant now,
                Instant expiresAt) {
            return true;
        }

        @Override
        public void release(TenantCode tenant, String scope, String owner) {
            operations.add("release-lease");
        }
    }
}
