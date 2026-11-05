package com.vetsync.pms.adapter.out.persistence;

import com.vetsync.pms.adapter.out.persistence.entity.AuditEntry;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.repository.AuditMasterDataRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Tag("integration")
class PmsDatabaseIntegrationTests {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("vetsync")
            .withUsername("vetsync_test")
            .withPassword("synthetic_test_password")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private AuditMasterDataRepository auditMasterDataRepository;

    @Test
    void flywayCreatesEveryBusinessAndAuditTable() {
        Set<String> tableNames = jdbcTemplate.queryForList("SHOW TABLES", String.class)
                .stream()
                .collect(Collectors.toSet());

        assertThat(tableNames).containsExactlyInAnyOrder(
                "clinic", "staff", "client", "species", "breed", "patient", "visit",
                "medical_record_entry", "medication", "prescription", "prescription_item",
                "invoice", "invoice_line", "payment", "audit_master_data", "audit_visit",
                "audit_prescription", "audit_invoice", "pms_user", "flyway_schema_history");
    }

    @Test
    void clinicRepositoryPersistsAndTriggersCaptureInsertUpdateAndFirstSoftDelete() {
        ClinicEntity clinic = clinicRepository.saveAndFlush(
                new ClinicEntity("SW", "Synthetic clinic", "Sample City", "1 Example Road",
                        "+359000000000", "clinic@example.invalid", true));

        ClinicEntity reloaded = clinicRepository.findByTenantIdAndPublicId("SW", clinic.getPublicId())
                .orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Synthetic clinic");
        assertThat(reloaded.getId()).isEqualTo(clinic.getId());
        assertThat(reloaded.getTenantId()).isEqualTo("SW");

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET @pms_changed_by = 'integration-test'");
            }
            try (PreparedStatement updateName = connection.prepareStatement(
                    "UPDATE clinic SET name = ? WHERE id = ?");
                 PreparedStatement softDelete = connection.prepareStatement(
                         "UPDATE clinic SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?")) {
                updateName.setString(1, "Updated clinic");
                updateName.setLong(2, clinic.getId());
                updateName.executeUpdate();
                softDelete.setLong(1, clinic.getId());
                softDelete.executeUpdate();
            }
            return null;
        });

        var audit = auditMasterDataRepository
                .findAllByTenantIdAndChangedAtGreaterThanEqualOrderByChangedAtAscAuditIdAsc("SW",
                        Instant.EPOCH)
                .stream()
                .filter(entry -> entry.getPublicId().equals(clinic.getPublicId()))
                .toList();

        assertThat(audit).extracting(AuditEntry::getOperation)
                .containsExactly(AuditEntry.Operation.INSERT, AuditEntry.Operation.UPDATE, AuditEntry.Operation.DELETE);
        assertThat(audit.get(2).getRowVersion()).isEqualTo(2);
        assertThat(audit.get(1).getChangedBy()).isEqualTo("integration-test");
        assertThat(audit.get(2).getChangedAt()).isNotNull();
        assertThat(audit.get(2).getEntityId()).isEqualTo(clinic.getId());
        assertThat(audit.get(2).getPayload().get("tenant_id").asText()).isEqualTo("SW");
        assertThat(audit.get(2).getPayload().get("deleted_at").isNull()).isFalse();
        assertThat(audit.get(2).getPayload().get("name").asText()).isEqualTo("Updated clinic");
    }

    @Test
    void triggersRouteAllBusinessChangesToTheirConfiguredAuditStreams() {
        String tenant = "SE";
        long clinicId = insertClinic(tenant);
        long clientId = insertClient(tenant, clinicId);
        long speciesId = insertSpecies(tenant);
        long patientId = insertPatient(tenant, clinicId, clientId, speciesId);
        long staffId = insertStaff(tenant, clinicId);
        long visitId = insertVisit(tenant, clinicId, patientId, staffId);
        long prescriptionId = insertPrescription(tenant, patientId, visitId, staffId);
        long medicationId = insertMedication(tenant);
        long invoiceId = insertInvoice(tenant, clinicId, clientId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO patient
                    (tenant_id, public_id, client_id, clinic_id, name, species_id, status)
                VALUES ('SW', ?, ?, ?, 'Cross-tenant patient', ?, 'ACTIVE')
                """, UUID.randomUUID().toString(), clientId, clinicId, speciesId))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertMedicalRecordEntry(tenant, patientId, visitId);
        insertPrescriptionItem(tenant, prescriptionId, medicationId);
        insertInvoiceLine(tenant, invoiceId);
        insertPayment(tenant, invoiceId);

        assertThat(auditTypes("audit_master_data", tenant))
                .contains("clinic", "client", "patient")
                .doesNotContain("staff", "species", "breed");
        assertThat(auditTypes("audit_visit", tenant))
                .contains("visit", "medical_record_entry");
        assertThat(auditTypes("audit_prescription", tenant))
                .contains("prescription", "prescription_item")
                .doesNotContain("medication");
        assertThat(auditTypes("audit_invoice", tenant))
                .contains("invoice", "invoice_line", "payment");
    }

    private long insertClinic(String tenant) {
        jdbcTemplate.update("INSERT INTO clinic (tenant_id, public_id, name, active) VALUES (?, ?, ?, ?)",
                tenant, UUID.randomUUID().toString(), "Audit fixture clinic", true);
        return lastId("clinic", tenant);
    }

    private long insertClient(String tenant, long clinicId) {
        jdbcTemplate.update("""
                INSERT INTO client (tenant_id, public_id, registered_clinic_id, first_name, last_name)
                VALUES (?, ?, ?, ?, ?)
                """, tenant, UUID.randomUUID().toString(), clinicId, "Sample", "Owner");
        return lastId("client", tenant);
    }

    private long insertSpecies(String tenant) {
        jdbcTemplate.update("INSERT INTO species (tenant_id, public_id, code, name) VALUES (?, ?, ?, ?)",
                tenant, UUID.randomUUID().toString(), "DOG", "Dog");
        return lastId("species", tenant);
    }

    private long insertPatient(String tenant, long clinicId, long clientId, long speciesId) {
        jdbcTemplate.update("""
                INSERT INTO patient
                    (tenant_id, public_id, client_id, clinic_id, name, species_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, tenant, UUID.randomUUID().toString(), clientId, clinicId, "Sample patient", speciesId, "ACTIVE");
        return lastId("patient", tenant);
    }

    private long insertStaff(String tenant, long clinicId) {
        jdbcTemplate.update("""
                INSERT INTO staff (tenant_id, public_id, clinic_id, role, full_name, active)
                VALUES (?, ?, ?, 'VET', 'Sample Vet', TRUE)
                """, tenant, UUID.randomUUID().toString(), clinicId);
        return lastId("staff", tenant);
    }

    private long insertVisit(String tenant, long clinicId, long patientId, long staffId) {
        jdbcTemplate.update("""
                INSERT INTO visit
                    (tenant_id, public_id, patient_id, clinic_id, vet_id, visit_type, status, scheduled_at)
                VALUES (?, ?, ?, ?, ?, 'CHECKUP', 'SCHEDULED', CURRENT_TIMESTAMP(6))
                """, tenant, UUID.randomUUID().toString(), patientId, clinicId, staffId);
        return lastId("visit", tenant);
    }

    private long insertPrescription(String tenant, long patientId, long visitId, long staffId) {
        jdbcTemplate.update("""
                INSERT INTO prescription
                    (tenant_id, public_id, patient_id, visit_id, vet_id, status, issued_on, valid_until)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY))
                """, tenant, UUID.randomUUID().toString(), patientId, visitId, staffId);
        return lastId("prescription", tenant);
    }

    private long insertMedication(String tenant) {
        jdbcTemplate.update("""
                INSERT INTO medication (tenant_id, public_id, code, name, form, strength, controlled)
                VALUES (?, ?, 'SYNTH-001', 'Synthetic medication', 'tablet', '10 mg', FALSE)
                """, tenant, UUID.randomUUID().toString());
        return lastId("medication", tenant);
    }

    private long insertInvoice(String tenant, long clinicId, long clientId) {
        jdbcTemplate.update("""
                INSERT INTO invoice
                    (tenant_id, public_id, clinic_id, client_id, invoice_no, status,
                     issued_on, due_on, total_amount, paid_amount)
                VALUES (?, ?, ?, ?, 'INV-SYNTH-001', 'ISSUED', CURRENT_DATE,
                        DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 10.00, 0.00)
                """, tenant, UUID.randomUUID().toString(), clinicId, clientId);
        return lastId("invoice", tenant);
    }

    private void insertMedicalRecordEntry(String tenant, long patientId, long visitId) {
        jdbcTemplate.update("""
                INSERT INTO medical_record_entry
                    (tenant_id, public_id, patient_id, visit_id, entry_type, description, recorded_at, recorded_by)
                VALUES (?, ?, ?, ?, 'NOTE', 'Synthetic note', CURRENT_TIMESTAMP(6), 'Integration test')
                """, tenant, UUID.randomUUID().toString(), patientId, visitId);
    }

    private void insertPrescriptionItem(String tenant, long prescriptionId, long medicationId) {
        jdbcTemplate.update("""
                INSERT INTO prescription_item
                    (tenant_id, public_id, prescription_id, medication_id, dosage, frequency,
                     duration_days, quantity, refills_allowed)
                VALUES (?, ?, ?, ?, '1 tablet', 'once daily', 5, 5, 0)
                """, tenant, UUID.randomUUID().toString(), prescriptionId, medicationId);
    }

    private void insertInvoiceLine(String tenant, long invoiceId) {
        jdbcTemplate.update("""
                INSERT INTO invoice_line
                    (tenant_id, public_id, invoice_id, description, quantity, unit_price, vat_rate)
                VALUES (?, ?, ?, 'Synthetic service', 1.000, 10.00, 20.00)
                """, tenant, UUID.randomUUID().toString(), invoiceId);
    }

    private void insertPayment(String tenant, long invoiceId) {
        jdbcTemplate.update("""
                INSERT INTO payment
                    (tenant_id, public_id, invoice_id, paid_at, amount, method)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), 10.00, 'CARD')
                """, tenant, UUID.randomUUID().toString(), invoiceId);
    }

    private long lastId(String table, String tenant) {
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM " + table + " WHERE tenant_id = ?",
                Long.class, tenant);
    }

    private Set<String> auditTypes(String auditTable, String tenant) {
        return Set.copyOf(jdbcTemplate.queryForList(
                "SELECT DISTINCT entity_type FROM " + auditTable + " WHERE tenant_id = ?",
                String.class, tenant));
    }
}
