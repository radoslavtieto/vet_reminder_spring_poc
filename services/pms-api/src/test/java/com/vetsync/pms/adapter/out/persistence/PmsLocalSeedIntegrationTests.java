package com.vetsync.pms.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class PmsLocalSeedIntegrationTests {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("vetsync_seed_test")
            .withUsername("vetsync_seed_test")
            .withPassword("synthetic_seed_password")
            .withCommand("--log-bin-trust-function-creators=1");

    @Test
    void localSeedPopulatesOnlyAnEmptyMigratedDatabaseAndIsIdempotent() {
        DataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("""
                INSERT INTO clinic (tenant_id, public_id, name, active)
                VALUES ('NC', '00000000-0000-0000-0000-000000000001', 'Existing local clinic', TRUE)
                """);
        runLocalSeed(dataSource);

        assertThat(count(jdbcTemplate, "clinic")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinic WHERE name = 'Sample Veterinary Clinic'", Integer.class))
                .isZero();

        flyway.clean();
        flyway.migrate();

        runLocalSeed(dataSource);
        Map<String, Integer> seededCounts = snapshotCounts(jdbcTemplate);

        assertThat(seededCounts).containsEntry("clinic", 1)
                .containsEntry("staff", 1)
                .containsEntry("client", 1)
                .containsEntry("species", 1)
                .containsEntry("breed", 1)
                .containsEntry("patient", 1)
                .containsEntry("visit", 1)
                .containsEntry("medical_record_entry", 1)
                .containsEntry("medication", 1)
                .containsEntry("prescription", 1)
                .containsEntry("prescription_item", 1)
                .containsEntry("invoice", 1)
                .containsEntry("invoice_line", 1)
                .containsEntry("payment", 1);

        runLocalSeed(dataSource);

        assertThat(snapshotCounts(jdbcTemplate)).isEqualTo(seededCounts);
    }

    private void runLocalSeed(DataSource dataSource) {
        new ResourceDatabasePopulator(new ClassPathResource("db/seed/local-seed.sql"))
                .execute(dataSource);
    }

    private int count(JdbcTemplate jdbcTemplate, String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private Map<String, Integer> snapshotCounts(JdbcTemplate jdbcTemplate) {
        return Map.ofEntries(
                Map.entry("clinic", count(jdbcTemplate, "clinic")),
                Map.entry("staff", count(jdbcTemplate, "staff")),
                Map.entry("client", count(jdbcTemplate, "client")),
                Map.entry("species", count(jdbcTemplate, "species")),
                Map.entry("breed", count(jdbcTemplate, "breed")),
                Map.entry("patient", count(jdbcTemplate, "patient")),
                Map.entry("visit", count(jdbcTemplate, "visit")),
                Map.entry("medical_record_entry", count(jdbcTemplate, "medical_record_entry")),
                Map.entry("medication", count(jdbcTemplate, "medication")),
                Map.entry("prescription", count(jdbcTemplate, "prescription")),
                Map.entry("prescription_item", count(jdbcTemplate, "prescription_item")),
                Map.entry("invoice", count(jdbcTemplate, "invoice")),
                Map.entry("invoice_line", count(jdbcTemplate, "invoice_line")),
                Map.entry("payment", count(jdbcTemplate, "payment")));
    }
}
