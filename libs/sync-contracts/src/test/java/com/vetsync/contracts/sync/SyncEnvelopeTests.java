package com.vetsync.contracts.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncEnvelopeTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesVersionOneEnvelopeWithContractFieldNames() throws Exception {
        SyncEnvelope envelope = new SyncEnvelope(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SW",
                "master-data",
                "CLIENT",
                UUID.randomUUID(),
                SyncOperation.UPSERT,
                7,
                Instant.parse("2026-10-30T10:00:00Z"),
                Instant.parse("2026-10-30T10:00:05Z"),
                Map.of("name", "Synthetic Client"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(envelope));

        assertThat(json.get("schemaVersion").intValue()).isEqualTo(1);
        assertThat(json.get("tenant").textValue()).isEqualTo("SW");
        assertThat(json.get("stream").textValue()).isEqualTo("master-data");
        assertThat(json.get("operation").textValue()).isEqualTo("UPSERT");
        assertThat(json.get("occurredAt").textValue()).isEqualTo("2026-10-30T10:00:00Z");
        assertThat(json.get("payload").get("name").textValue()).isEqualTo("Synthetic Client");
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertThatThrownBy(() -> new SyncEnvelope(
                2, UUID.randomUUID(), UUID.randomUUID(), "SW", "master-data", "CLIENT", UUID.randomUUID(),
                SyncOperation.UPSERT, 1, Instant.EPOCH, Instant.EPOCH, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void makesPayloadImmutable() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("name", "Synthetic Client");

        SyncEnvelope envelope = new SyncEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), "SW", "master-data", "CLIENT", UUID.randomUUID(),
                SyncOperation.UPSERT, 1, Instant.EPOCH, Instant.EPOCH, payload);
        payload.put("name", "Changed");

        assertThat(envelope.payload()).containsEntry("name", "Synthetic Client");
    }
}
