package com.vetsync.sync.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.sync.adapter.out.messaging.ServiceBusEventPublisher;
import com.vetsync.sync.adapter.out.messaging.ServiceBusEventQuarantine;
import com.vetsync.sync.application.EventPublisher;
import com.vetsync.sync.application.RetryingEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "sync.messaging.service-bus", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({ServiceBusProperties.class, EventDeliveryProperties.class})
public class ServiceBusConfiguration {

    @Bean
    ServiceBusSenderClient syncServiceBusSender(ServiceBusProperties properties) {
        return sender(properties, properties.queue());
    }

    @Bean
    ServiceBusSenderClient syncServiceBusQuarantineSender(ServiceBusProperties properties) {
        return sender(properties, properties.quarantineQueue());
    }

    private ServiceBusSenderClient sender(ServiceBusProperties properties, String queueName) {
        ServiceBusClientBuilder builder = new ServiceBusClientBuilder();
        if (!properties.connectionString().isEmpty()) {
            builder.connectionString(properties.connectionString());
        } else {
            builder
                    .fullyQualifiedNamespace(properties.namespace())
                    .credential(new DefaultAzureCredentialBuilder().build());
        }
        return builder.sender()
                .queueName(queueName)
                .buildClient();
    }

    @Bean
    EventPublisher serviceBusEventPublisher(
            @Qualifier("syncServiceBusSender") ServiceBusSenderClient syncServiceBusSender,
            @Qualifier("syncServiceBusQuarantineSender") ServiceBusSenderClient syncServiceBusQuarantineSender,
            EventDeliveryProperties deliveryProperties,
            ObjectMapper objectMapper) {
        return new RetryingEventPublisher(
                new ServiceBusEventPublisher(syncServiceBusSender, objectMapper),
                new ServiceBusEventQuarantine(syncServiceBusQuarantineSender, objectMapper),
                deliveryProperties.maxAttempts(),
                deliveryProperties.retryDelay(),
                RetryingEventPublisher.threadSleepDelay());
    }
}
