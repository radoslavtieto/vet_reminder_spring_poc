package com.vetsync.sync.adapter.out.mysql;

import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlTimeWindowSourceTests {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Sofia");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);
    private static final UUID FIRST_ID = UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c");
    private static final UUID SECOND_ID = UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c");

    private JdbcTemplate jdbcTemplate;
    private MySqlTimeWindowSource source;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE prescription (
                    id BIGINT PRIMARY KEY, tenant_id CHAR(2), public_id CHAR(36), version BIGINT,
                    deleted_at TIMESTAMP NULL, status VARCHAR(20), valid_until DATE, notes VARCHAR(100)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE invoice (
                    id BIGINT PRIMARY KEY, tenant_id CHAR(2), public_id CHAR(36), version BIGINT,
                    deleted_at TIMESTAMP NULL, status VARCHAR(20), due_on DATE,
                    total_amount DECIMAL(12,2), paid_amount DECIMAL(12,2), invoice_no VARCHAR(60)
                )
                """);
        source = new MySqlTimeWindowSource(
                jdbcTemplate,
                Clock.fixed(Instant.parse("2026-10-30T22:30:00Z"), BUSINESS_ZONE));
    }

    @Test
    void selectsActivePrescriptionsThroughThirtyDaysAheadAndAllPastExpiryDates() {
        insertPrescription(1, "SW", "ACTIVE", LocalDate.of(2026, 11, 30), null, FIRST_ID);
        insertPrescription(2, "SW", "ACTIVE", LocalDate.of(2026, 12, 1), null, SECOND_ID);
        insertPrescription(3, "SW", "ACTIVE", LocalDate.of(2026, 10, 15), null, SECOND_ID);
        insertPrescription(4, "SE", "ACTIVE", LocalDate.of(2026, 10, 20), null, FIRST_ID);
        insertPrescription(5, "SW", "EXPIRED", LocalDate.of(2026, 10, 20), null, FIRST_ID);
        insertPrescription(6, "SW", "ACTIVE", LocalDate.of(2026, 10, 20),
                Date.valueOf(TODAY.minusDays(1)), SECOND_ID);

        List<com.vetsync.sync.domain.TimeWindowRecord> records = source.findEligible(
                command(SyncStream.PRESCRIPTION_EXPIRY), TODAY, 0);

        assertThat(records).extracting(com.vetsync.sync.domain.TimeWindowRecord::sourceId).containsExactly(1L, 3L);
        assertThat(records.get(0).entityType()).isEqualTo("PRESCRIPTION");
        assertThat(records.get(0).occurredAt())
                .isEqualTo(LocalDate.of(2026, 11, 30).atStartOfDay(BUSINESS_ZONE).toInstant());
        assertThat(records.get(0).payload()).containsEntry("status", "ACTIVE");
    }

    @Test
    void selectsUnpaidInvoicesDueInLastSixMonthsAfterSevenDayGrace() {
        insertInvoice(1, "SW", "ISSUED", LocalDate.of(2026, 4, 29), "10.00", "0.00", null, FIRST_ID);
        insertInvoice(2, "SW", "ISSUED", LocalDate.of(2026, 4, 30), "10.00", "0.00", null, FIRST_ID);
        insertInvoice(3, "SW", "ISSUED", LocalDate.of(2026, 10, 24), "10.00", "0.00", null, FIRST_ID);
        insertInvoice(4, "SW", "ISSUED", LocalDate.of(2026, 10, 23), "10.00", "0.00", null, SECOND_ID);
        insertInvoice(5, "SW", "PARTIALLY_PAID", LocalDate.of(2026, 10, 1), "10.00", "5.00", null, FIRST_ID);
        insertInvoice(6, "SW", "PAID", LocalDate.of(2026, 10, 1), "10.00", "10.00", null, SECOND_ID);
        insertInvoice(7, "SE", "ISSUED", LocalDate.of(2026, 10, 1), "10.00", "0.00", null, FIRST_ID);
        insertInvoice(8, "SW", "ISSUED", LocalDate.of(2026, 10, 1), "10.00", "0.00",
                Date.valueOf(TODAY.minusDays(1)), SECOND_ID);

        List<com.vetsync.sync.domain.TimeWindowRecord> records = source.findEligible(
                command(SyncStream.UNPAID_INVOICES), TODAY, 0);

        assertThat(records).extracting(com.vetsync.sync.domain.TimeWindowRecord::sourceId).containsExactly(2L, 4L, 5L);
        assertThat(records.get(1).occurredAt())
                .isEqualTo(LocalDate.of(2026, 10, 31).atStartOfDay(BUSINESS_ZONE).toInstant());
        assertThat(records.get(2).payload()).containsEntry("invoice_no", "INV-5");
    }

    private static TimeWindowScanCommand command(SyncStream stream) {
        return new TimeWindowScanCommand(
                TenantCode.SW, stream, 20, 30, 6, 7, "worker-1", Duration.ofMinutes(5));
    }

    private void insertPrescription(
            long id,
            String tenant,
            String status,
            LocalDate validUntil,
            Date deletedAt,
            UUID publicId) {
        jdbcTemplate.update(
                "INSERT INTO prescription VALUES (?, ?, ?, 3, ?, ?, ?, 'Synthetic notes')",
                id,
                tenant,
                publicId.toString(),
                deletedAt,
                status,
                Date.valueOf(validUntil));
    }

    private void insertInvoice(
            long id,
            String tenant,
            String status,
            LocalDate dueOn,
            String total,
            String paid,
            Date deletedAt,
            UUID publicId) {
        jdbcTemplate.update(
                "INSERT INTO invoice VALUES (?, ?, ?, 4, ?, ?, ?, ?, ?, ?)",
                id,
                tenant,
                publicId.toString(),
                deletedAt,
                status,
                Date.valueOf(dueOn),
                new BigDecimal(total),
                new BigDecimal(paid),
                "INV-" + id);
    }
}
