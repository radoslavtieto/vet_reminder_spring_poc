package com.vetsync.sync.adapter.out.messaging;

import com.vetsync.contracts.SyncEnvelope;
import com.vetsync.sync.application.SyncEventPublisher;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitSyncEventPublisher implements SyncEventPublisher {
    private static final String EXCHANGE = "vetsync.sync";
    private static final String ROUTING_KEY = "master-data";

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitSyncEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${sync.messaging.confirm-timeout:PT10S}") Duration confirmTimeout
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(SyncEnvelope event) {
        CorrelationData correlation = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("Broker rejected event " + event.eventId() + ": " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("Event " + event.eventId() + " was not routed to a queue");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting broker confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Broker confirmation failed for event " + event.eventId(), exception);
        }
    }
}
