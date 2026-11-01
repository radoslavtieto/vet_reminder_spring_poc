package com.vetsync.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.sync.adapter.out.mysql.MySqlAuditSource;
import com.vetsync.sync.adapter.out.mysql.MySqlSnapshotSource;
import com.vetsync.sync.adapter.out.mysql.MySqlTimeWindowSource;
import com.vetsync.sync.application.AuditSource;
import com.vetsync.sync.application.SnapshotSource;
import com.vetsync.sync.application.TimeWindowSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "sync.source.mysql", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MySqlSourceProperties.class)
public class MySqlSourceConfiguration {

    @Bean
    DataSource pmsDataSource(MySqlSourceProperties properties) {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(properties.url())
                .username(properties.username())
                .password(properties.password())
                .build();
    }

    @Bean
    JdbcTemplate pmsJdbcTemplate(DataSource pmsDataSource) {
        return new JdbcTemplate(pmsDataSource);
    }

    @Bean
    AuditSource mySqlAuditSource(JdbcTemplate pmsJdbcTemplate, ObjectMapper objectMapper) {
        return new MySqlAuditSource(pmsJdbcTemplate, objectMapper);
    }

    @Bean
    TimeWindowSource mySqlTimeWindowSource(JdbcTemplate pmsJdbcTemplate, java.time.Clock syncClock) {
        return new MySqlTimeWindowSource(pmsJdbcTemplate, syncClock);
    }

    @Bean
    SnapshotSource mySqlSnapshotSource(JdbcTemplate pmsJdbcTemplate) {
        return new MySqlSnapshotSource(pmsJdbcTemplate);
    }
}
