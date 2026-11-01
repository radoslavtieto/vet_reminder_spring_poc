package com.vetsync.sync.adapter.out.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.application.EventQuarantine;

import java.util.Map;
import java.util.Objects;

public final class ServiceBusEventQuarantine implements EventQuarantine {

    private final ServiceBusSenderClient sender;
    private final ObjectMapper objectMapper;

    public ServiceBusEventQuarantine(ServiceBusSenderClient sender, ObjectMapper objectMapper) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void quarantine(SyncEnvelope envelope, String failureReason) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of("event", envelope, "failureReason", failureReason));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize quarantined sync event " + envelope.eventId(), exception);
        }
        sender.sendMessage(new ServiceBusMessage(BinaryData.fromBytes(body))
                .setMessageId(envelope.eventId() + ":quarantine")
                .setCorrelationId(envelope.correlationId().toString())
                .setSubject("sync-quarantine")
                .setContentType("application/json"));
    }
}
