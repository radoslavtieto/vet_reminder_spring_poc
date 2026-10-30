SELECT
    NOT EXISTS (SELECT 1 FROM clinic LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM staff LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM client LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM species LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM breed LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM patient LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM visit LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM medical_record_entry LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM medication LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM prescription LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM prescription_item LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM invoice LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM invoice_line LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM payment LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM audit_master_data LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM audit_visit LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM audit_prescription LIMIT 1)
    AND NOT EXISTS (SELECT 1 FROM audit_invoice LIMIT 1)
INTO @should_seed;

SELECT IF(@should_seed, 'Empty database; inserting synthetic local sample data',
    'Database already contains data; skipping local seed') AS seed_status;

START TRANSACTION;

INSERT INTO clinic (tenant_id, public_id, name, city, address, phone, email, active)
SELECT 'SW', UUID(), 'Sample Veterinary Clinic', 'Sample City', '1 Example Road',
       '+359000000001', 'clinic@example.invalid', TRUE
WHERE @should_seed;

INSERT INTO staff (tenant_id, public_id, clinic_id, role, full_name, active)
SELECT 'SW', UUID(), clinic.id, 'VET', 'Alex Sample', TRUE
FROM clinic
WHERE @should_seed AND clinic.tenant_id = 'SW' AND clinic.name = 'Sample Veterinary Clinic';

INSERT INTO client
    (tenant_id, public_id, registered_clinic_id, first_name, last_name, email, phone, city, preferred_contact)
SELECT 'SW', UUID(), clinic.id, 'Jamie', 'Example', 'owner@example.invalid',
       '+359000000002', 'Sample City', 'EMAIL'
FROM clinic
WHERE @should_seed AND clinic.tenant_id = 'SW' AND clinic.name = 'Sample Veterinary Clinic';

INSERT INTO species (tenant_id, public_id, code, name)
SELECT 'SW', UUID(), 'DOG', 'Dog'
WHERE @should_seed;

INSERT INTO breed (tenant_id, public_id, species_id, code, name)
SELECT 'SW', UUID(), species.id, 'LABRADOR', 'Labrador Retriever'
FROM species
WHERE @should_seed AND species.tenant_id = 'SW' AND species.code = 'DOG';

INSERT INTO patient
    (tenant_id, public_id, client_id, clinic_id, name, species_id, breed_id, sex,
     birth_date, microchip_no, neutered, weight_kg, status)
SELECT 'SW', UUID(), client.id, clinic.id, 'Milo', species.id, breed.id, 'MALE',
       '2021-04-12', 'LOCAL-SW-0001', TRUE, 18.50, 'ACTIVE'
FROM clinic
JOIN client ON client.tenant_id = clinic.tenant_id
JOIN species ON species.tenant_id = clinic.tenant_id
JOIN breed ON breed.tenant_id = species.tenant_id AND breed.species_id = species.id
WHERE @should_seed
  AND clinic.tenant_id = 'SW' AND clinic.name = 'Sample Veterinary Clinic'
  AND client.first_name = 'Jamie' AND client.last_name = 'Example'
  AND species.code = 'DOG' AND breed.code = 'LABRADOR';

INSERT INTO visit
    (tenant_id, public_id, patient_id, clinic_id, vet_id, visit_type, status,
     scheduled_at, started_at, completed_at, reason, diagnosis, notes)
SELECT 'SW', UUID(), patient.id, clinic.id, staff.id, 'CHECKUP', 'COMPLETED',
       CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
       CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
       CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
       'Routine wellness examination', 'Healthy', 'Synthetic local seed record'
FROM clinic
JOIN staff ON staff.tenant_id = clinic.tenant_id AND staff.clinic_id = clinic.id
JOIN patient ON patient.tenant_id = clinic.tenant_id AND patient.clinic_id = clinic.id
WHERE @should_seed
  AND clinic.tenant_id = 'SW' AND clinic.name = 'Sample Veterinary Clinic'
  AND staff.full_name = 'Alex Sample' AND patient.name = 'Milo';

INSERT INTO medical_record_entry
    (tenant_id, public_id, patient_id, visit_id, entry_type, description, recorded_at, recorded_by)
SELECT 'SW', UUID(), patient.id, visit.id, 'NOTE', 'Synthetic wellness examination record',
       CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY, 'Alex Sample'
FROM patient
JOIN visit ON visit.tenant_id = patient.tenant_id AND visit.patient_id = patient.id
WHERE @should_seed AND patient.tenant_id = 'SW' AND patient.name = 'Milo';

INSERT INTO medication (tenant_id, public_id, code, name, form, strength, controlled)
SELECT 'SW', UUID(), 'SAMPLE-001', 'Synthetic veterinary supplement', 'tablet', '100 mg', FALSE
WHERE @should_seed;

INSERT INTO prescription
    (tenant_id, public_id, patient_id, visit_id, vet_id, status, issued_on, valid_until, notes)
SELECT 'SW', UUID(), patient.id, visit.id, staff.id, 'ACTIVE', CURRENT_DATE,
       DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 'Synthetic local seed prescription'
FROM patient
JOIN visit ON visit.tenant_id = patient.tenant_id AND visit.patient_id = patient.id
JOIN staff ON staff.tenant_id = patient.tenant_id AND staff.clinic_id = patient.clinic_id
WHERE @should_seed AND patient.tenant_id = 'SW' AND patient.name = 'Milo'
  AND staff.full_name = 'Alex Sample';

INSERT INTO prescription_item
    (tenant_id, public_id, prescription_id, medication_id, dosage, frequency,
     duration_days, quantity, refills_allowed)
SELECT 'SW', UUID(), prescription.id, medication.id, '1 tablet', 'once daily', 14, 14, 0
FROM prescription
JOIN medication ON medication.tenant_id = prescription.tenant_id
WHERE @should_seed AND prescription.tenant_id = 'SW'
  AND medication.code = 'SAMPLE-001';

INSERT INTO invoice
    (tenant_id, public_id, clinic_id, client_id, visit_id, invoice_no, status,
     issued_on, due_on, total_amount, paid_amount)
SELECT 'SW', UUID(), clinic.id, client.id, visit.id, 'LOCAL-SW-INV-0001', 'PARTIALLY_PAID',
       CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 75.00, 25.00
FROM clinic
JOIN client ON client.tenant_id = clinic.tenant_id
JOIN visit ON visit.tenant_id = clinic.tenant_id AND visit.clinic_id = clinic.id
WHERE @should_seed
  AND clinic.tenant_id = 'SW' AND clinic.name = 'Sample Veterinary Clinic'
  AND client.first_name = 'Jamie' AND client.last_name = 'Example'
  AND visit.patient_id = (SELECT id FROM patient WHERE tenant_id = 'SW' AND name = 'Milo');

INSERT INTO invoice_line
    (tenant_id, public_id, invoice_id, description, quantity, unit_price, vat_rate)
SELECT 'SW', UUID(), invoice.id, 'Wellness examination', 1.000, 75.00, 20.00
FROM invoice
WHERE @should_seed AND invoice.tenant_id = 'SW' AND invoice.invoice_no = 'LOCAL-SW-INV-0001';

INSERT INTO payment (tenant_id, public_id, invoice_id, paid_at, amount, method)
SELECT 'SW', UUID(), invoice.id, CURRENT_TIMESTAMP(6), 25.00, 'CARD'
FROM invoice
WHERE @should_seed AND invoice.tenant_id = 'SW' AND invoice.invoice_no = 'LOCAL-SW-INV-0001';

COMMIT;
