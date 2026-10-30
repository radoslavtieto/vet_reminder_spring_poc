CREATE TABLE audit_master_data (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id CHAR(2) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    public_id CHAR(36) NOT NULL,
    operation ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    row_version BIGINT NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    changed_by VARCHAR(100) NULL,
    payload JSON NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_changed_at (changed_at),
    KEY idx_audit_master_tenant_changed (tenant_id, changed_at, audit_id)
) ENGINE=InnoDB;

CREATE TABLE audit_visit (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id CHAR(2) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    public_id CHAR(36) NOT NULL,
    operation ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    row_version BIGINT NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    changed_by VARCHAR(100) NULL,
    payload JSON NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_changed_at (changed_at),
    KEY idx_audit_visit_tenant_changed (tenant_id, changed_at, audit_id)
) ENGINE=InnoDB;

CREATE TABLE audit_prescription (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id CHAR(2) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    public_id CHAR(36) NOT NULL,
    operation ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    row_version BIGINT NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    changed_by VARCHAR(100) NULL,
    payload JSON NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_changed_at (changed_at),
    KEY idx_audit_prescription_tenant_changed (tenant_id, changed_at, audit_id)
) ENGINE=InnoDB;

CREATE TABLE audit_invoice (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id CHAR(2) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    public_id CHAR(36) NOT NULL,
    operation ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    row_version BIGINT NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    changed_by VARCHAR(100) NULL,
    payload JSON NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_changed_at (changed_at),
    KEY idx_audit_invoice_tenant_changed (tenant_id, changed_at, audit_id)
) ENGINE=InnoDB;

DELIMITER $$

CREATE PROCEDURE write_audit_master_data(
    IN p_tenant_id CHAR(2), IN p_entity_type VARCHAR(40), IN p_entity_id BIGINT,
    IN p_public_id CHAR(36), IN p_operation VARCHAR(6), IN p_row_version BIGINT,
    IN p_changed_by VARCHAR(100), IN p_payload JSON
)
BEGIN
    INSERT INTO audit_master_data
        (tenant_id, entity_type, entity_id, public_id, operation, row_version, changed_by, payload)
    VALUES
        (p_tenant_id, p_entity_type, p_entity_id, p_public_id, p_operation, p_row_version,
         NULLIF(p_changed_by, ''), p_payload);
END$$

CREATE PROCEDURE write_audit_visit(
    IN p_tenant_id CHAR(2), IN p_entity_type VARCHAR(40), IN p_entity_id BIGINT,
    IN p_public_id CHAR(36), IN p_operation VARCHAR(6), IN p_row_version BIGINT,
    IN p_changed_by VARCHAR(100), IN p_payload JSON
)
BEGIN
    INSERT INTO audit_visit
        (tenant_id, entity_type, entity_id, public_id, operation, row_version, changed_by, payload)
    VALUES
        (p_tenant_id, p_entity_type, p_entity_id, p_public_id, p_operation, p_row_version,
         NULLIF(p_changed_by, ''), p_payload);
END$$

CREATE PROCEDURE write_audit_prescription(
    IN p_tenant_id CHAR(2), IN p_entity_type VARCHAR(40), IN p_entity_id BIGINT,
    IN p_public_id CHAR(36), IN p_operation VARCHAR(6), IN p_row_version BIGINT,
    IN p_changed_by VARCHAR(100), IN p_payload JSON
)
BEGIN
    INSERT INTO audit_prescription
        (tenant_id, entity_type, entity_id, public_id, operation, row_version, changed_by, payload)
    VALUES
        (p_tenant_id, p_entity_type, p_entity_id, p_public_id, p_operation, p_row_version,
         NULLIF(p_changed_by, ''), p_payload);
END$$

CREATE PROCEDURE write_audit_invoice(
    IN p_tenant_id CHAR(2), IN p_entity_type VARCHAR(40), IN p_entity_id BIGINT,
    IN p_public_id CHAR(36), IN p_operation VARCHAR(6), IN p_row_version BIGINT,
    IN p_changed_by VARCHAR(100), IN p_payload JSON
)
BEGIN
    INSERT INTO audit_invoice
        (tenant_id, entity_type, entity_id, public_id, operation, row_version, changed_by, payload)
    VALUES
        (p_tenant_id, p_entity_type, p_entity_id, p_public_id, p_operation, p_row_version,
         NULLIF(p_changed_by, ''), p_payload);
END$$

CREATE TRIGGER clinic_version_before_update
BEFORE UPDATE ON clinic FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER clinic_audit_after_insert
AFTER INSERT ON clinic FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'clinic', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'name', NEW.name, 'city', NEW.city,
            'address', NEW.address, 'phone', NEW.phone, 'email', NEW.email, 'active', NEW.active));
END$$
CREATE TRIGGER clinic_audit_after_update
AFTER UPDATE ON clinic FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'clinic', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'name', NEW.name, 'city', NEW.city,
            'address', NEW.address, 'phone', NEW.phone, 'email', NEW.email, 'active', NEW.active));
END$$

CREATE TRIGGER staff_version_before_update
BEFORE UPDATE ON staff FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$

CREATE TRIGGER client_version_before_update
BEFORE UPDATE ON client FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER client_audit_after_insert
AFTER INSERT ON client FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'client', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'registered_clinic_id', NEW.registered_clinic_id,
            'first_name', NEW.first_name, 'last_name', NEW.last_name, 'email', NEW.email,
            'phone', NEW.phone, 'address', NEW.address, 'city', NEW.city,
            'preferred_contact', NEW.preferred_contact, 'gdpr_consent_at', NEW.gdpr_consent_at));
END$$
CREATE TRIGGER client_audit_after_update
AFTER UPDATE ON client FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'client', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'registered_clinic_id', NEW.registered_clinic_id,
            'first_name', NEW.first_name, 'last_name', NEW.last_name, 'email', NEW.email,
            'phone', NEW.phone, 'address', NEW.address, 'city', NEW.city,
            'preferred_contact', NEW.preferred_contact, 'gdpr_consent_at', NEW.gdpr_consent_at));
END$$

CREATE TRIGGER species_version_before_update
BEFORE UPDATE ON species FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$

CREATE TRIGGER breed_version_before_update
BEFORE UPDATE ON breed FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$

CREATE TRIGGER patient_version_before_update
BEFORE UPDATE ON patient FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER patient_audit_after_insert
AFTER INSERT ON patient FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'patient', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'client_id', NEW.client_id, 'clinic_id', NEW.clinic_id,
            'name', NEW.name, 'species_id', NEW.species_id, 'breed_id', NEW.breed_id,
            'sex', NEW.sex, 'birth_date', NEW.birth_date, 'microchip_no', NEW.microchip_no,
            'neutered', NEW.neutered, 'weight_kg', NEW.weight_kg, 'status', NEW.status,
            'deceased_at', NEW.deceased_at));
END$$
CREATE TRIGGER patient_audit_after_update
AFTER UPDATE ON patient FOR EACH ROW
BEGIN
    CALL write_audit_master_data(NEW.tenant_id, 'patient', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'client_id', NEW.client_id, 'clinic_id', NEW.clinic_id,
            'name', NEW.name, 'species_id', NEW.species_id, 'breed_id', NEW.breed_id,
            'sex', NEW.sex, 'birth_date', NEW.birth_date, 'microchip_no', NEW.microchip_no,
            'neutered', NEW.neutered, 'weight_kg', NEW.weight_kg, 'status', NEW.status,
            'deceased_at', NEW.deceased_at));
END$$

CREATE TRIGGER visit_version_before_update
BEFORE UPDATE ON visit FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER visit_audit_after_insert
AFTER INSERT ON visit FOR EACH ROW
BEGIN
    CALL write_audit_visit(NEW.tenant_id, 'visit', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'clinic_id', NEW.clinic_id,
            'vet_id', NEW.vet_id, 'visit_type', NEW.visit_type, 'status', NEW.status,
            'scheduled_at', NEW.scheduled_at, 'started_at', NEW.started_at,
            'completed_at', NEW.completed_at, 'reason', NEW.reason, 'diagnosis', NEW.diagnosis,
            'notes', NEW.notes));
END$$
CREATE TRIGGER visit_audit_after_update
AFTER UPDATE ON visit FOR EACH ROW
BEGIN
    CALL write_audit_visit(NEW.tenant_id, 'visit', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'clinic_id', NEW.clinic_id,
            'vet_id', NEW.vet_id, 'visit_type', NEW.visit_type, 'status', NEW.status,
            'scheduled_at', NEW.scheduled_at, 'started_at', NEW.started_at,
            'completed_at', NEW.completed_at, 'reason', NEW.reason, 'diagnosis', NEW.diagnosis,
            'notes', NEW.notes));
END$$

CREATE TRIGGER medical_record_entry_version_before_update
BEFORE UPDATE ON medical_record_entry FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER medical_record_entry_audit_after_insert
AFTER INSERT ON medical_record_entry FOR EACH ROW
BEGIN
    CALL write_audit_visit(NEW.tenant_id, 'medical_record_entry', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'visit_id', NEW.visit_id,
            'entry_type', NEW.entry_type, 'description', NEW.description, 'recorded_at', NEW.recorded_at,
            'recorded_by', NEW.recorded_by));
END$$
CREATE TRIGGER medical_record_entry_audit_after_update
AFTER UPDATE ON medical_record_entry FOR EACH ROW
BEGIN
    CALL write_audit_visit(NEW.tenant_id, 'medical_record_entry', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'visit_id', NEW.visit_id,
            'entry_type', NEW.entry_type, 'description', NEW.description, 'recorded_at', NEW.recorded_at,
            'recorded_by', NEW.recorded_by));
END$$

CREATE TRIGGER medication_version_before_update
BEFORE UPDATE ON medication FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$

CREATE TRIGGER prescription_version_before_update
BEFORE UPDATE ON prescription FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER prescription_audit_after_insert
AFTER INSERT ON prescription FOR EACH ROW
BEGIN
    CALL write_audit_prescription(NEW.tenant_id, 'prescription', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'visit_id', NEW.visit_id,
            'vet_id', NEW.vet_id, 'status', NEW.status, 'issued_on', NEW.issued_on,
            'valid_until', NEW.valid_until, 'notes', NEW.notes));
END$$
CREATE TRIGGER prescription_audit_after_update
AFTER UPDATE ON prescription FOR EACH ROW
BEGIN
    CALL write_audit_prescription(NEW.tenant_id, 'prescription', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'patient_id', NEW.patient_id, 'visit_id', NEW.visit_id,
            'vet_id', NEW.vet_id, 'status', NEW.status, 'issued_on', NEW.issued_on,
            'valid_until', NEW.valid_until, 'notes', NEW.notes));
END$$

CREATE TRIGGER prescription_item_version_before_update
BEFORE UPDATE ON prescription_item FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER prescription_item_audit_after_insert
AFTER INSERT ON prescription_item FOR EACH ROW
BEGIN
    CALL write_audit_prescription(NEW.tenant_id, 'prescription_item', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'prescription_id', NEW.prescription_id,
            'medication_id', NEW.medication_id, 'dosage', NEW.dosage, 'frequency', NEW.frequency,
            'duration_days', NEW.duration_days, 'quantity', NEW.quantity,
            'refills_allowed', NEW.refills_allowed));
END$$
CREATE TRIGGER prescription_item_audit_after_update
AFTER UPDATE ON prescription_item FOR EACH ROW
BEGIN
    CALL write_audit_prescription(NEW.tenant_id, 'prescription_item', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'prescription_id', NEW.prescription_id,
            'medication_id', NEW.medication_id, 'dosage', NEW.dosage, 'frequency', NEW.frequency,
            'duration_days', NEW.duration_days, 'quantity', NEW.quantity,
            'refills_allowed', NEW.refills_allowed));
END$$

CREATE TRIGGER invoice_version_before_update
BEFORE UPDATE ON invoice FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER invoice_audit_after_insert
AFTER INSERT ON invoice FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'invoice', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'clinic_id', NEW.clinic_id, 'client_id', NEW.client_id,
            'visit_id', NEW.visit_id, 'invoice_no', NEW.invoice_no, 'status', NEW.status,
            'issued_on', NEW.issued_on, 'due_on', NEW.due_on, 'currency', NEW.currency,
            'total_amount', NEW.total_amount, 'paid_amount', NEW.paid_amount));
END$$
CREATE TRIGGER invoice_audit_after_update
AFTER UPDATE ON invoice FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'invoice', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'clinic_id', NEW.clinic_id, 'client_id', NEW.client_id,
            'visit_id', NEW.visit_id, 'invoice_no', NEW.invoice_no, 'status', NEW.status,
            'issued_on', NEW.issued_on, 'due_on', NEW.due_on, 'currency', NEW.currency,
            'total_amount', NEW.total_amount, 'paid_amount', NEW.paid_amount));
END$$

CREATE TRIGGER invoice_line_version_before_update
BEFORE UPDATE ON invoice_line FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER invoice_line_audit_after_insert
AFTER INSERT ON invoice_line FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'invoice_line', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'invoice_id', NEW.invoice_id, 'description', NEW.description,
            'quantity', NEW.quantity, 'unit_price', NEW.unit_price, 'vat_rate', NEW.vat_rate));
END$$
CREATE TRIGGER invoice_line_audit_after_update
AFTER UPDATE ON invoice_line FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'invoice_line', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'invoice_id', NEW.invoice_id, 'description', NEW.description,
            'quantity', NEW.quantity, 'unit_price', NEW.unit_price, 'vat_rate', NEW.vat_rate));
END$$

CREATE TRIGGER payment_version_before_update
BEFORE UPDATE ON payment FOR EACH ROW
BEGIN
    SET NEW.version = OLD.version + 1;
END$$
CREATE TRIGGER payment_audit_after_insert
AFTER INSERT ON payment FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'payment', NEW.id, NEW.public_id, 'INSERT',
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'invoice_id', NEW.invoice_id, 'paid_at', NEW.paid_at,
            'amount', NEW.amount, 'method', NEW.method));
END$$
CREATE TRIGGER payment_audit_after_update
AFTER UPDATE ON payment FOR EACH ROW
BEGIN
    CALL write_audit_invoice(NEW.tenant_id, 'payment', NEW.id, NEW.public_id,
        IF(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL, 'DELETE', 'UPDATE'),
        NEW.version, @pms_changed_by,
        JSON_OBJECT('id', NEW.id, 'tenant_id', NEW.tenant_id, 'public_id', NEW.public_id,
            'created_at', NEW.created_at, 'updated_at', NEW.updated_at, 'version', NEW.version,
            'deleted_at', NEW.deleted_at, 'invoice_id', NEW.invoice_id, 'paid_at', NEW.paid_at,
            'amount', NEW.amount, 'method', NEW.method));
END$$

DELIMITER ;
