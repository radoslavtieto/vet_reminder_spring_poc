package com.vetsync.sync.adapter.out.mysql;

import com.vetsync.sync.application.SnapshotSource;
import com.vetsync.sync.domain.SnapshotRecord;
import com.vetsync.sync.domain.SnapshotSyncCommand;
import com.vetsync.contracts.sync.SyncOperation;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MySqlSnapshotSource implements SnapshotSource {

    private final JdbcTemplate jdbcTemplate;

    public MySqlSnapshotSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public List<SnapshotRecord> findSnapshotPage(SnapshotSyncCommand command, long afterSourceId) {
        if (afterSourceId < 0) {
            throw new IllegalArgumentException("afterSourceId must not be negative");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM %s
                WHERE tenant_id = ?
                  AND id > ?
                """.formatted(command.entity().tableName()));
        List<Object> parameters = new ArrayList<>();
        parameters.add(command.tenant().name());
        parameters.add(afterSourceId);
        if (command.publicId() != null) {
            sql.append(" AND public_id = ?");
            parameters.add(command.publicId().toString());
        }
        if (command.updatedFromInclusive() != null) {
            sql.append(" AND updated_at >= ? AND updated_at < ?");
            parameters.add(Timestamp.from(command.updatedFromInclusive()));
            parameters.add(Timestamp.from(command.updatedToExclusive()));
        }
        sql.append(" ORDER BY id ASC LIMIT ?");
        parameters.add(command.batchSize());

        return jdbcTemplate.query(sql.toString(), statement -> {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
        }, (resultSet, rowNumber) -> toRecord(resultSet, command));
    }

    private SnapshotRecord toRecord(ResultSet resultSet, SnapshotSyncCommand command) throws SQLException {
        long sourceId = resultSet.getLong("id");
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt == null) {
            throw new IllegalStateException("Snapshot row " + sourceId + " has no updated_at timestamp");
        }
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        return new SnapshotRecord(
                sourceId,
                UUID.fromString(resultSet.getString("public_id").trim()),
                command.entity().entityType(),
                resultSet.getLong("version"),
                updatedAt.toInstant(),
                deletedAt == null ? SyncOperation.UPSERT : SyncOperation.DELETE,
                toPayload(resultSet));
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
