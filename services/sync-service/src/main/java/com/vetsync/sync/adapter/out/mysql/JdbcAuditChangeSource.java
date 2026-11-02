package com.vetsync.sync.adapter.out.mysql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.sync.application.AuditChangeSource;
import com.vetsync.sync.domain.AuditChange;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAuditChangeSource implements AuditChangeSource {
    private static final String QUERY = """
            SELECT a.audit_id, a.tenant_id, a.entity_type, a.public_id, a.operation,
                   a.row_version, a.changed_at, a.payload, c.public_id AS owner_public_id,
                   s.code AS species_code
            FROM audit_master_data a
            LEFT JOIN client c
              ON a.entity_type = 'patient'
             AND c.tenant_id = a.tenant_id
             AND c.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(a.payload, '$.client_id')) AS UNSIGNED)
            LEFT JOIN species s
              ON a.entity_type = 'patient'
             AND s.tenant_id = a.tenant_id
             AND s.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(a.payload, '$.species_id')) AS UNSIGNED)
            WHERE a.tenant_id = ?
              AND a.changed_at < ?
              AND (a.audit_id > ? OR a.changed_at >= ?)
            ORDER BY a.audit_id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAuditChangeSource(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AuditChange> readMasterData(
            String tenant, long lastAuditId, Instant overlapFrom, Instant safeBefore, int batchSize) {
        return jdbcTemplate.query(QUERY, rowMapper(), tenant, Timestamp.from(safeBefore), lastAuditId,
                Timestamp.from(overlapFrom), batchSize);
    }

    private RowMapper<AuditChange> rowMapper() {
        return (result, rowNumber) -> {
            try {
                var payload = objectMapper.readTree(result.getString("payload"));
                if ("patient".equals(result.getString("entity_type"))) {
                    String ownerPublicId = result.getString("owner_public_id");
                    if (ownerPublicId == null) {
                        throw new SQLException("Patient audit row has no matching client");
                    }
                    if (!payload.isObject()) {
                        throw new SQLException("Patient audit payload is not a JSON object");
                    }
                    String speciesCode = result.getString("species_code");
                    if (speciesCode == null) {
                        throw new SQLException("Patient audit row has no matching species");
                    }
                    ((com.fasterxml.jackson.databind.node.ObjectNode) payload)
                            .put("owner_public_id", ownerPublicId.trim())
                            .put("species_code", speciesCode);
                }
                return new AuditChange(
                        result.getLong("audit_id"), result.getString("tenant_id"),
                        result.getString("entity_type"), UUID.fromString(result.getString("public_id").trim()),
                        result.getString("operation"), result.getLong("row_version"),
                        result.getTimestamp("changed_at").toInstant(), payload
                );
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                throw new SQLException("Invalid payload in audit_master_data row " + rowNumber, exception);
            }
        };
    }
}
