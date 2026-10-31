package com.vetsync.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncMessagingConfiguration {
    @Bean
    TopicExchange syncExchange(@Value("${sync.messaging.exchange:vetsync.sync}") String exchange) {
        return new TopicExchange(exchange, true, false);
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
