package com.vetsync.sync.adapter.out.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.sync.SyncEnvelope;
import com.vetsync.sync.application.EventPublisher;

import java.util.Objects;

public final class ServiceBusEventPublisher implements EventPublisher {

    private final ServiceBusSenderClient sender;
    private final ObjectMapper objectMapper;

    public ServiceBusEventPublisher(ServiceBusSenderClient sender, ObjectMapper objectMapper) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void publish(SyncEnvelope envelope) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize sync event " + envelope.eventId(), exception);
        }

        ServiceBusMessage message = new ServiceBusMessage(body)
                .setMessageId(envelope.eventId().toString())
                .setCorrelationId(envelope.correlationId().toString())
                .setSubject(envelope.stream())
                .setContentType("application/json");
        sender.sendMessage(message);
    }
}
