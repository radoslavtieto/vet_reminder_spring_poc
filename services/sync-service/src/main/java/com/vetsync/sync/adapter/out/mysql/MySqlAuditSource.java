package com.vetsync.sync.adapter.out.mysql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.sync.SyncOperation;
import com.vetsync.sync.application.AuditSource;
import com.vetsync.sync.domain.AuditChange;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MySqlAuditSource implements AuditSource {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MySqlAuditSource(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<AuditChange> findChanges(
            TenantCode tenant,
            SyncStream stream,
            long fromAuditIdInclusive,
            Instant changedBefore,
            int limit) {
        if (fromAuditIdInclusive < 1) {
            throw new IllegalArgumentException("fromAuditIdInclusive must be positive");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        String tableName = tableNameFor(stream);
        String sql = """
                SELECT audit_id, entity_type, public_id, operation, row_version, changed_at, payload
                FROM %s
                WHERE tenant_id = ? AND audit_id >= ? AND changed_at < ?
                ORDER BY audit_id ASC
                LIMIT ?
                """.formatted(tableName);
        return jdbcTemplate.query(sql, statement -> {
            statement.setString(1, tenant.name());
            statement.setLong(2, fromAuditIdInclusive);
            statement.setTimestamp(3, Timestamp.from(changedBefore));
            statement.setInt(4, limit);
        }, (resultSet, rowNumber) -> {
            long auditId = resultSet.getLong("audit_id");
            return new AuditChange(
                    auditId,
                    resultSet.getString("entity_type").toUpperCase(Locale.ROOT),
                    UUID.fromString(resultSet.getString("public_id").trim()),
                    toSyncOperation(resultSet.getString("operation")),
                    resultSet.getLong("row_version"),
                    toInstant(resultSet.getTimestamp("changed_at"), auditId),
                    readPayload(resultSet.getString("payload"), auditId));
        });
    }

    private String tableNameFor(SyncStream stream) {
        return switch (stream) {
            case MASTER_DATA -> "audit_master_data";
            case VISITS -> "audit_visit";
            case PRESCRIPTIONS -> "audit_prescription";
            case INVOICES -> "audit_invoice";
            case PRESCRIPTION_EXPIRY, UNPAID_INVOICES ->
                    throw new IllegalArgumentException("Stream " + stream.contractName() + " does not have an audit table");
        };
    }

    private SyncOperation toSyncOperation(String operation) {
        return switch (operation) {
            case "INSERT", "UPDATE" -> SyncOperation.UPSERT;
            case "DELETE" -> SyncOperation.DELETE;
            default -> throw new IllegalStateException("Unsupported PMS audit operation: " + operation);
        };
    }

    private Instant toInstant(Timestamp timestamp, long auditId) {
        if (timestamp == null) {
            throw new IllegalStateException("Audit row " + auditId + " has no changed_at timestamp");
        }
        return timestamp.toInstant();
    }

    private Map<String, Object> readPayload(String json, long auditId) {
        try {
            Map<String, Object> payload = objectMapper.readValue(json, PAYLOAD_TYPE);
            if (payload == null) {
                throw new IllegalStateException("Audit row " + auditId + " has a null payload");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Audit row " + auditId + " has an invalid JSON payload", exception);
        }
    }
}
