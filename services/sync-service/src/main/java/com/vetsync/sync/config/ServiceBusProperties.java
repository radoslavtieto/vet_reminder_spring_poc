package com.vetsync.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sync.messaging.service-bus")
public record ServiceBusProperties(
        String connectionString,
        String namespace,
        String queue,
        String quarantineQueue) {

    public ServiceBusProperties {
        connectionString = normalize(connectionString);
        namespace = normalize(namespace);
        queue = requireText(queue, "SERVICEBUS_QUEUE");
        quarantineQueue = requireText(quarantineQueue, "SERVICEBUS_QUARANTINE_QUEUE");
        if (queue.equals(quarantineQueue)) {
            throw new IllegalStateException("SERVICEBUS_QUARANTINE_QUEUE must differ from SERVICEBUS_QUEUE");
        }
        if (connectionString.isEmpty() == namespace.isEmpty()) {
            throw new IllegalStateException(
                    "Configure exactly one of SERVICEBUS_CONNECTION_STRING or SERVICEBUS_NAMESPACE");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireText(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be configured when Service Bus is enabled");
        }
        return value;
    }
}
