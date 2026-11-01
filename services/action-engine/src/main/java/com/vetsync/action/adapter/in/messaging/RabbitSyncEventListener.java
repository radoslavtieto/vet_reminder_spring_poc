package com.vetsync.action.adapter.in.messaging;

import com.vetsync.action.application.SyncEventProcessor;
import com.vetsync.contracts.SyncEnvelope;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "action.messaging.enabled", havingValue = "true")
public class RabbitSyncEventListener {
    private final SyncEventProcessor processor;

    public RabbitSyncEventListener(SyncEventProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "vetsync.action-engine")
    public void receive(SyncEnvelope event) {
        processor.process(event);
    }
}
