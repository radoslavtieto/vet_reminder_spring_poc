package com.vetsync.sync.adapter.out.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.sync.SyncOperation;
import com.vetsync.sync.domain.AuditChange;
import com.vetsync.sync.domain.SyncStream;
import com.vetsync.sync.domain.TenantCode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlAuditSourceTests {

    private static final Instant CUTOFF = Instant.parse("2026-10-30T10:00:00Z");
    private static final UUID FIRST_PUBLIC_ID = UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c");
    private static final UUID SECOND_PUBLIC_ID = UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c");

    private JdbcTemplate jdbcTemplate;
    private MySqlAuditSource auditSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE audit_master_data (
                    audit_id BIGINT PRIMARY KEY,
                    tenant_id CHAR(2) NOT NULL,
                    entity_type VARCHAR(40) NOT NULL,
                    entity_id BIGINT NOT NULL,
                    public_id CHAR(36) NOT NULL,
                    operation VARCHAR(6) NOT NULL,
                    row_version BIGINT NOT NULL,
                    changed_at TIMESTAMP(6) NOT NULL,
                    payload CLOB NOT NULL
                )
                """);
        auditSource = new MySqlAuditSource(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void readsOnlyRequestedTenantAndSafeOrderedRowsAndMapsOperations() {
        insert(1, "SW", "INSERT", FIRST_PUBLIC_ID, CUTOFF.minusSeconds(1), "{\"name\":\"Old\"}");
        insert(2, "SW", "UPDATE", FIRST_PUBLIC_ID, CUTOFF.minusNanos(1_000), "{\"name\":\"Current\"}");
        insert(3, "SE", "INSERT", SECOND_PUBLIC_ID, CUTOFF.minusSeconds(1), "{\"name\":\"Other tenant\"}");
        insert(4, "SW", "INSERT", SECOND_PUBLIC_ID, CUTOFF, "{\"name\":\"Not yet safe\"}");
        insert(5, "SW", "DELETE", SECOND_PUBLIC_ID, CUTOFF.minusSeconds(1), "{\"name\":\"Deleted\"}");

        List<AuditChange> changes = auditSource.findChanges(
                TenantCode.SW, SyncStream.MASTER_DATA, 2, CUTOFF, 10);

        assertThat(changes)
                .extracting(AuditChange::auditId)
                .containsExactly(2L, 5L);
        assertThat(changes.get(0).entityType()).isEqualTo("CLIENT");
        assertThat(changes.get(0).operation()).isEqualTo(SyncOperation.UPSERT);
        assertThat(changes.get(0).publicId()).isEqualTo(FIRST_PUBLIC_ID);
        assertThat(changes.get(0).payload()).containsEntry("name", "Current");
        assertThat(changes.get(1).operation()).isEqualTo(SyncOperation.DELETE);
    }

    @Test
    void rejectsTimeWindowStreamsInsteadOfChoosingAnAuditTable() {
        assertThatThrownBy(() -> auditSource.findChanges(
                TenantCode.SW, SyncStream.UNPAID_INVOICES, 1, CUTOFF, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not have an audit table");
    }

    private void insert(
            long auditId,
            String tenant,
            String operation,
            UUID publicId,
            Instant changedAt,
            String payload) {
        jdbcTemplate.update(
                """
                INSERT INTO audit_master_data
                    (audit_id, tenant_id, entity_type, entity_id, public_id, operation,
                     row_version, changed_at, payload)
                VALUES (?, ?, 'client', ?, ?, ?, 3, ?, ?)
                """,
                auditId,
                tenant,
                auditId,
                publicId.toString(),
                operation,
                Timestamp.from(changedAt),
                payload);
    }
}
