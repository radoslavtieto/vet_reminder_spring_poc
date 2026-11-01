package com.vetsync.sync.adapter.out.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.contracts.sync.SyncOperation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServiceBusEventPublisherTests {

    private final ServiceBusSenderClient sender = mock(ServiceBusSenderClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ServiceBusEventPublisher publisher = new ServiceBusEventPublisher(sender, objectMapper);

    @Test
    void publishesContractJsonWithStableBrokerMetadata() throws Exception {
        SyncEnvelope envelope = envelope();
        ArgumentCaptor<ServiceBusMessage> messageCaptor = ArgumentCaptor.forClass(ServiceBusMessage.class);

        publisher.publish(envelope);

        verify(sender).sendMessage(messageCaptor.capture());
        ServiceBusMessage message = messageCaptor.getValue();
        assertThat(message.getMessageId()).isEqualTo(envelope.eventId().toString());
        assertThat(message.getCorrelationId()).isEqualTo(envelope.correlationId().toString());
        assertThat(message.getSubject()).isEqualTo("master-data");
        assertThat(message.getContentType()).isEqualTo("application/json");
        assertThat(message.getBody().toBytes()).containsExactly(objectMapper.writeValueAsBytes(envelope));
    }

    @Test
    void surfacesBrokerPublishFailures() {
        doThrow(new IllegalStateException("broker unavailable"))
                .when(sender).sendMessage(any(ServiceBusMessage.class));

        assertThatThrownBy(() -> publisher.publish(envelope()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");
    }

    private SyncEnvelope envelope() {
        return new SyncEnvelope(
                1,
                UUID.fromString("f1e2d3c4-b5a6-4789-8abc-def012345678"),
                UUID.fromString("a1b2c3d4-e5f6-4789-8abc-def012345678"),
                "SW",
                "master-data",
                "CLIENT",
                UUID.fromString("0d2f7448-e4f7-4c75-9934-04fcac86a19c"),
                SyncOperation.UPSERT,
                3,
                Instant.parse("2026-10-30T10:00:00Z"),
                Instant.parse("2026-10-30T10:00:05Z"),
                Map.of("name", "Synthetic Client"));
    }
}
