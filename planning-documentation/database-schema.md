# Implemented database schemas

These diagrams reflect the checked-in Flyway migrations. They show implemented tables and declared foreign-key relationships, not the full column/index definitions. PMS business relationships are tenant-scoped: foreign keys include `tenant_id`, and each referenced `(tenant_id, id)` pair is unique.

## PMS MySQL

Source: `services/pms-api/src/main/resources/db/migration/V1__create_shared_pms_schema.sql`, `V2__create_audit_streams_and_triggers.sql`, and `V3__create_pms_users.sql`.

```mermaid
erDiagram
  CLINIC {
    BIGINT id PK
    CHAR tenant_id
    CHAR public_id
  }
  STAFF {
    BIGINT id PK
    CHAR tenant_id
    BIGINT clinic_id FK
  }
  PMS_USER {
    BIGINT id PK
    CHAR tenant_id
    BIGINT staff_id FK
  }
  CLIENT {
    BIGINT id PK
    CHAR tenant_id
    BIGINT registered_clinic_id FK
  }
  SPECIES {
    BIGINT id PK
    CHAR tenant_id
  }
  BREED {
    BIGINT id PK
    CHAR tenant_id
    BIGINT species_id FK
  }
  PATIENT {
    BIGINT id PK
    CHAR tenant_id
    BIGINT client_id FK
    BIGINT clinic_id FK
    BIGINT species_id FK
    BIGINT breed_id FK
  }
  VISIT {
    BIGINT id PK
    CHAR tenant_id
    BIGINT patient_id FK
    BIGINT clinic_id FK
    BIGINT vet_id FK
  }
  MEDICAL_RECORD_ENTRY {
    BIGINT id PK
    CHAR tenant_id
    BIGINT patient_id FK
    BIGINT visit_id FK
  }
  MEDICATION {
    BIGINT id PK
    CHAR tenant_id
  }
  PRESCRIPTION {
    BIGINT id PK
    CHAR tenant_id
    BIGINT patient_id FK
    BIGINT visit_id FK
    BIGINT vet_id FK
  }
  PRESCRIPTION_ITEM {
    BIGINT id PK
    CHAR tenant_id
    BIGINT prescription_id FK
    BIGINT medication_id FK
  }
  INVOICE {
    BIGINT id PK
    CHAR tenant_id
    BIGINT clinic_id FK
    BIGINT client_id FK
    BIGINT visit_id FK
  }
  INVOICE_LINE {
    BIGINT id PK
    CHAR tenant_id
    BIGINT invoice_id FK
  }
  PAYMENT {
    BIGINT id PK
    CHAR tenant_id
    BIGINT invoice_id FK
  }
  AUDIT_MASTER_DATA {
    BIGINT audit_id PK
    CHAR tenant_id
    VARCHAR entity_type
    JSON payload
  }
  AUDIT_VISIT {
    BIGINT audit_id PK
    CHAR tenant_id
    VARCHAR entity_type
    JSON payload
  }
  AUDIT_PRESCRIPTION {
    BIGINT audit_id PK
    CHAR tenant_id
    VARCHAR entity_type
    JSON payload
  }
  AUDIT_INVOICE {
    BIGINT audit_id PK
    CHAR tenant_id
    VARCHAR entity_type
    JSON payload
  }

  CLINIC ||--o{ STAFF : employs
  STAFF o|--o{ PMS_USER : authenticates
  CLINIC ||--o{ CLIENT : registers
  CLINIC ||--o{ PATIENT : hosts
  CLIENT ||--o{ PATIENT : owns
  SPECIES ||--o{ BREED : classifies
  SPECIES ||--o{ PATIENT : classifies
  BREED o|--o{ PATIENT : describes
  PATIENT ||--o{ VISIT : has
  CLINIC ||--o{ VISIT : hosts
  STAFF ||--o{ VISIT : attends
  PATIENT ||--o{ MEDICAL_RECORD_ENTRY : has
  VISIT o|--o{ MEDICAL_RECORD_ENTRY : relates
  PATIENT ||--o{ PRESCRIPTION : receives
  VISIT ||--o{ PRESCRIPTION : issues
  STAFF ||--o{ PRESCRIPTION : prescribes
  PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
  MEDICATION ||--o{ PRESCRIPTION_ITEM : supplies
  CLINIC ||--o{ INVOICE : issues
  CLIENT ||--o{ INVOICE : billed
  VISIT o|--o{ INVOICE : relates
  INVOICE ||--o{ INVOICE_LINE : contains
  INVOICE ||--o{ PAYMENT : receives
```

Audit rows are snapshots and have no foreign-key constraints to business rows. The stream tables capture:

| Audit table | Triggered entities |
|-------------|--------------------|
| `audit_master_data` | `clinic`, `client`, `patient` |
| `audit_visit` | `visit`, `medical_record_entry` |
| `audit_prescription` | `prescription`, `prescription_item` |
| `audit_invoice` | `invoice`, `invoice_line`, `payment` |

`staff`, `species`, `breed`, and `medication` are not currently written to these audit streams. The sync service currently consumes only `audit_master_data`.

## Action Engine PostgreSQL

Source: `services/action-engine/src/main/resources/db/migration/V1__create_action_engine_domain.sql`.

```mermaid
erDiagram
  SITE {
    VARCHAR tenant PK
    UUID source_id PK
    BIGINT source_version
    VARCHAR display_name
    BOOLEAN active
  }
  CONTACT {
    VARCHAR tenant PK
    UUID source_id PK
    BIGINT source_version
    VARCHAR display_name
    BOOLEAN consent_given
    BOOLEAN active
  }
  CARE_SUBJECT {
    VARCHAR tenant PK
    UUID source_id PK
    BIGINT source_version
    VARCHAR owner_source_id
    VARCHAR species_code
    BOOLEAN active
  }
  PROCESSED_EVENT {
    UUID event_id PK
    VARCHAR tenant
    TIMESTAMPTZ processed_at
  }
```

The Action Engine has no foreign keys between these initial tables. `care_subject.owner_source_id` stores the source contact identifier; it is not a PostgreSQL foreign key. Treatment, care-event, receivable, action, and action-attempt tables are not implemented.
