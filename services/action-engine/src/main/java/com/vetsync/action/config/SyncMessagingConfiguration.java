package com.vetsync.action.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class SyncMessagingConfiguration {
    @Bean
    TopicExchange syncExchange() {
        return new TopicExchange("vetsync.sync", true, false);
    }

    @Bean
    DirectExchange syncDeadLetterExchange() {
        return new DirectExchange("vetsync.sync.dlx", true, false);
    }

    @Bean
    Queue actionEventQueue() {
        return QueueBuilder.durable("vetsync.action-engine")
                .deadLetterExchange("vetsync.sync.dlx")
                .deadLetterRoutingKey("action-engine.dead")
                .build();
    }

    @Bean
    Queue actionDeadLetterQueue() {
        return QueueBuilder.durable("vetsync.action-engine.dlq").build();
    }

    @Bean
    Binding actionEventBinding(Queue actionEventQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(actionEventQueue).to(syncExchange).with("master-data");
    }

    @Bean
    Binding actionDeadLetterBinding(Queue actionDeadLetterQueue, DirectExchange syncDeadLetterExchange) {
        return BindingBuilder.bind(actionDeadLetterQueue).to(syncDeadLetterExchange).with("action-engine.dead");
    }

    @Bean
    Jackson2JsonMessageConverter syncMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.vetsync.contracts");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
