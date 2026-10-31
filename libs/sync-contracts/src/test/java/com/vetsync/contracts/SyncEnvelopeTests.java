package com.vetsync.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncEnvelopeTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void serializesAndDeserializesTheVersionedEnvelope() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode().put("first_name", "Synthetic");
        SyncEnvelope envelope = new SyncEnvelope(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SW",
                "master-data",
                "CLIENT",
                UUID.randomUUID(),
                SyncOperation.UPSERT,
                3,
                Instant.parse("2026-10-31T12:00:00Z"),
                Instant.parse("2026-10-31T12:00:01Z"),
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        SyncEnvelope restored = objectMapper.readValue(json, SyncEnvelope.class);

        assertThat(restored).isEqualTo(envelope);
        assertThat(objectMapper.readTree(json).path("schemaVersion").intValue()).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedSchemaVersions() {
        Instant occurredAt = Instant.parse("2026-10-31T12:00:00Z");
        assertThatThrownBy(() -> new SyncEnvelope(
                2, UUID.randomUUID(), UUID.randomUUID(), "SW", "master-data", "CLIENT",
                UUID.randomUUID(), SyncOperation.UPSERT, 1, occurredAt, occurredAt,
                objectMapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidTenantCodes() {
        assertThatThrownBy(() -> new SyncEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), "XX", "master-data", "CLIENT",
                UUID.randomUUID(), SyncOperation.UPSERT, 1, Instant.EPOCH, Instant.EPOCH,
                objectMapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SyncEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), null, "master-data", "CLIENT",
                UUID.randomUUID(), SyncOperation.UPSERT, 1, Instant.EPOCH, Instant.EPOCH,
                objectMapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
