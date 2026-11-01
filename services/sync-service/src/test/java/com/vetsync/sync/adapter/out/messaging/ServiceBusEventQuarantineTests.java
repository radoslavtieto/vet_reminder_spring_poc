package com.vetsync.sync.adapter.out.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.contracts.sync.SyncOperation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServiceBusEventQuarantineTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ServiceBusSenderClient sender = mock(ServiceBusSenderClient.class);
    private final ServiceBusEventQuarantine quarantine = new ServiceBusEventQuarantine(sender, objectMapper);
    private final SyncEnvelope envelope = new SyncEnvelope(
            1,
            UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            UUID.fromString("1d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            "SW",
            "master-data",
            "CLIENT",
            UUID.fromString("2d2f7448-e4f7-4c75-9934-04fcac86a19c"),
            SyncOperation.UPSERT,
            7,
            Instant.parse("2026-10-31T10:00:00Z"),
            Instant.parse("2026-10-31T10:00:01Z"),
            Map.of("name", "Synthetic"));

    @Test
    void publishesOriginalEnvelopeAndFailureReasonWithStableMessageId() throws Exception {
        quarantine.quarantine(envelope, "IllegalStateException: broker rejected message");

        verify(sender).sendMessage(argThat(message -> {
            try {
                JsonNode body = objectMapper.readTree(message.getBody().toBytes());
                return message.getMessageId().equals(envelope.eventId() + ":quarantine")
                        && message.getContentType().equals("application/json")
                        && body.get("failureReason").asText().contains("broker rejected")
                        && body.get("event").get("eventId").asText().equals(envelope.eventId().toString());
            } catch (Exception exception) {
                return false;
            }
        }));
    }
}
