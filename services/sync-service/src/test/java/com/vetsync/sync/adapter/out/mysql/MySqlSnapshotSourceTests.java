package com.vetsync.sync.adapter.out.mysql;

import com.vetsync.sync.domain.SnapshotEntity;
import com.vetsync.sync.domain.SnapshotMode;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.sync.domain.TenantCode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlSnapshotSourceTests {

    private static final UUID FIRST_ID = UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c");
    private static final UUID SECOND_ID = UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c");
    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private MySqlSnapshotSource source;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE client (
                    id BIGINT PRIMARY KEY, tenant_id CHAR(2), public_id CHAR(36), version BIGINT,
                    created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP NULL,
                    first_name VARCHAR(100)
                )
                """);
        source = new MySqlSnapshotSource(jdbcTemplate);
    }

    @Test
    void pagesTenantRowsBySourceIdAndPreservesSoftDeletes() {
        insert(1, "SW", FIRST_ID, START, null, "A");
        insert(2, "SW", SECOND_ID, END, END, "B");
        insert(3, "SE", FIRST_ID, START, null, "C");

        var firstPage = source.findSnapshotPage(command(null, null, null, 1), 0);
        var secondPage = source.findSnapshotPage(command(null, null, null, 1), firstPage.get(0).sourceId());

        assertThat(firstPage).singleElement().satisfies(record -> {
            assertThat(record.sourceId()).isEqualTo(1);
            assertThat(record.entityType()).isEqualTo("CLIENT");
        });
        assertThat(secondPage).singleElement().satisfies(record -> {
            assertThat(record.sourceId()).isEqualTo(2);
            assertThat(record.operation().name()).isEqualTo("DELETE");
            assertThat(record.payload()).containsEntry("first_name", "B");
        });
    }

    @Test
    void filtersPartialSnapshotsByPublicIdAndHalfOpenUpdatedTimeRange() {
        insert(1, "SW", FIRST_ID, START, null, "A");
        insert(2, "SW", SECOND_ID, END, null, "B");
        insert(3, "SE", FIRST_ID, START, null, "C");

        var byId = source.findSnapshotPage(command(FIRST_ID, null, null, 10), 0);
        var byRange = source.findSnapshotPage(command(null, START, END, 10), 0);

        assertThat(byId).extracting(record -> record.sourceId()).containsExactly(1L);
        assertThat(byRange).extracting(record -> record.sourceId()).containsExactly(1L);
    }

    private static SnapshotSyncCommand command(
            UUID publicId,
            Instant updatedFrom,
            Instant updatedTo,
            int batchSize) {
        return new SnapshotSyncCommand(
                TenantCode.SW,
                SnapshotEntity.CLIENT,
                SnapshotMode.PARTIAL,
                "partial-case-1",
                publicId,
                updatedFrom,
                updatedTo,
                batchSize,
                "worker-1",
                Duration.ofMinutes(5));
    }

    private void insert(
            long id,
            String tenant,
            UUID publicId,
            Instant updatedAt,
            Instant deletedAt,
            String firstName) {
        jdbcTemplate.update(
                "INSERT INTO client VALUES (?, ?, ?, 7, ?, ?, ?, ?)",
                id,
                tenant,
                publicId.toString(),
                Timestamp.from(updatedAt),
                Timestamp.from(updatedAt),
                deletedAt == null ? null : Timestamp.from(deletedAt),
                firstName);
    }
}
