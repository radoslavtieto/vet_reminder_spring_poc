package com.vetsync.sync.adapter.out.mysql;

import com.vetsync.sync.application.TimeWindowSource;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import com.vetsync.sync.domain.TimeWindowRecord;
import com.vetsync.sync.domain.TimeWindowScanCommand;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MySqlTimeWindowSource implements TimeWindowSource {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public MySqlTimeWindowSource(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<TimeWindowRecord> findEligible(
            TimeWindowScanCommand command,
            LocalDate today,
            long afterSourceId) {
        if (afterSourceId < 0) {
            throw new IllegalArgumentException("afterSourceId must not be negative");
        }
        return switch (command.stream()) {
            case PRESCRIPTION_EXPIRY -> findExpiringPrescriptions(command, today, afterSourceId);
            case UNPAID_INVOICES -> findOverdueInvoices(command, today, afterSourceId);
            case MASTER_DATA, VISITS, PRESCRIPTIONS, INVOICES ->
                    throw new IllegalArgumentException("Audit stream is not a time-window scan");
        };
    }

    private List<TimeWindowRecord> findExpiringPrescriptions(
            TimeWindowScanCommand command,
            LocalDate today,
            long afterSourceId) {
        String sql = """
                SELECT *
                FROM prescription
                WHERE tenant_id = ?
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                  AND valid_until <= ?
                  AND id > ?
                ORDER BY id ASC
                LIMIT ?
                """;
        LocalDate expiryCutoff = today.plusDays(command.expiryLeadDays());
        return jdbcTemplate.query(sql, statement -> {
            statement.setString(1, command.tenant().name());
            statement.setDate(2, Date.valueOf(expiryCutoff));
            statement.setLong(3, afterSourceId);
            statement.setInt(4, command.batchSize());
        }, (resultSet, rowNumber) -> {
            LocalDate validUntil = resultSet.getDate("valid_until").toLocalDate();
            return new TimeWindowRecord(
                    resultSet.getLong("id"),
                    "PRESCRIPTION",
                    UUID.fromString(resultSet.getString("public_id").trim()),
                    resultSet.getLong("version"),
                    validUntil.atStartOfDay(clock.getZone()).toInstant(),
                    toPayload(resultSet));
        });
    }

    private List<TimeWindowRecord> findOverdueInvoices(
            TimeWindowScanCommand command,
            LocalDate today,
            long afterSourceId) {
        String sql = """
                SELECT *
                FROM invoice
                WHERE tenant_id = ?
                  AND status IN ('ISSUED', 'PARTIALLY_PAID')
                  AND deleted_at IS NULL
                  AND total_amount > paid_amount
                  AND due_on >= ?
                  AND due_on < ?
                  AND id > ?
                ORDER BY id ASC
                LIMIT ?
                """;
        LocalDate dueDateLookback = today.minusMonths(command.invoiceLookbackMonths());
        LocalDate graceCutoffExclusive = today.minusDays(command.invoiceGraceDays());
        return jdbcTemplate.query(sql, statement -> {
            statement.setString(1, command.tenant().name());
            statement.setDate(2, Date.valueOf(dueDateLookback));
            statement.setDate(3, Date.valueOf(graceCutoffExclusive));
            statement.setLong(4, afterSourceId);
            statement.setInt(5, command.batchSize());
        }, (resultSet, rowNumber) -> {
            LocalDate dueOn = resultSet.getDate("due_on").toLocalDate();
            LocalDate overdueSince = dueOn.plusDays(Math.addExact(command.invoiceGraceDays(), 1));
            return new TimeWindowRecord(
                    resultSet.getLong("id"),
                    "INVOICE",
                    UUID.fromString(resultSet.getString("public_id").trim()),
                    resultSet.getLong("version"),
                    overdueSince.atStartOfDay(clock.getZone()).toInstant(),
                    toPayload(resultSet));
        });
    }

    private Map<String, Object> toPayload(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            String name = metadata.getColumnLabel(column).toLowerCase(Locale.ROOT);
            payload.put(name, toPayloadValue(resultSet.getObject(column)));
        }
        return payload;
    }

    private Object toPayloadValue(Object value) throws SQLException {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Clob clob) {
            return clob.getSubString(1, Math.toIntExact(clob.length()));
        }
        return value;
    }
}
