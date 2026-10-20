# VetSync POC: Clinic PMS → Sync Service → Action Engine

A proof of concept for a **near-real-time synchronization pipeline** between a veterinary Practice Management System (PMS) and a downstream Action Engine, built with **Java + Spring Boot**, developed **test-first (TDD)**, hosted on **Azure**, and runnable locally on **Windows 11** and **Ubuntu**.

> **How to use this file:** this is the source of truth for humans and coding agents. Read sections 1–3 first, then work through the roadmap (section 12) one phase at a time. Section 14 lists open decisions and things that must be verified rather than assumed.

---

## 1. Goals and non-goals

**Goals**
- Three independently deployable Spring Boot applications with clear contracts between them.
- Test-driven development from the first line of code (red → green → refactor).
- Three environments: `local`, `uat`, `prod`, with the same code and different configuration.
- Runs on Windows 11 and Ubuntu using Docker and Maven.
- Agent-friendly repo: coding agents can build, test, and extend it safely.

**Non-goals (for the POC)**
- Real clinical correctness, real medication catalogues, real e-prescription integrations.
- Real personal data. **Use synthetic data only** (owners' personal data falls under GDPR).
- Full UI. APIs and tests only.

---

## 2. System overview

```mermaid
flowchart LR
  subgraph PMS["1. Source System: Core PMS"]
    API["pms-api<br/>Spring Boot REST"]
    MYSQL[("MySQL<br/>6 regional tenants<br/>+ audit tables")]
    API --> MYSQL
  end

  subgraph SYNC["2. Middle Sync Service"]
    WORKER["sync-service<br/>Spring Boot<br/>poller + listener"]
    NOSQL[("NoSQL<br/>pointers, leases,<br/>dedupe")]
    WORKER --> NOSQL
  end

  subgraph ACTION["3. Action Engine & Dispatcher"]
    ENGINE["action-engine<br/>Spring Boot"]
    PG[("PostgreSQL<br/>own domain model")]
    ENGINE --> PG
  end

  MYSQL -- "poll audit tables<br/>(read-only)" --> WORKER
  WORKER -- "SyncEnvelope events<br/>(Azure Service Bus)" --> ENGINE
  ENGINE -- "notifications<br/>(email/SMS stubs)" --> OUT(["Owners / staff"])
```

| # | App | Role | Data store |
|---|-----|------|-----------|
| 1 | `pms-api` | Provides region-scoped REST CRUD, tenant-aware persistence, and trigger-written audit tables. | MySQL (one shared schema, `tenant_id`) |
| 2 | `sync-service` | Reads audit tables and time-window queries, tracks progress, deduplicates, publishes events. | NoSQL (pointers, leases, dedupe) |
| 3 | `action-engine` | Consumes events, maps them into **its own domain**, decides and dispatches actions. | PostgreSQL |

**Key principle:** each app owns its data. The sync service has **read-only** access to MySQL. The action engine never touches MySQL and never imports PMS classes. It only knows the published event contract.

---

## 3. Technology choices

| Concern | Choice | Notes |
|---------|--------|-------|
| Language | Java 21 (LTS) | Java 25 is also LTS; stay on 21 unless there is a reason to move. Verify current guidance. |
| Framework | Spring Boot 3.5.x or newer | **Pin one version in the parent POM; verify the current stable release before starting.** |
| Build | Maven multi-module | Each GitHub Actions workflow installs Maven 3.9.9; the Maven Wrapper remains a scaffold follow-up. |
| Source DB | MySQL 8.4 LTS | Flyway migrations. |
| Sync state | MongoDB locally, Azure Cosmos DB for MongoDB API in UAT/PROD | See open decision D3. |
| Messaging | Azure Service Bus (queue or topic) | Local: Service Bus emulator or RabbitMQ behind an interface. See D4. |
| Action DB | PostgreSQL 16+ | Flyway migrations. |
| Hosting | Azure Container Apps (+ Container Apps **Jobs** for timers) | See D5. |
| IaC | Bicep | Or Terraform if your team prefers. |
| CI/CD | GitHub Actions | Build → test → image → deploy to UAT → manual approval → PROD. |
| Secrets | Azure Key Vault + managed identity | Never commit secrets. |
| Observability | Micrometer + OpenTelemetry → Azure Application Insights | Correlation ID on every event. |

**Test stack:** JUnit 5, AssertJ, Mockito, **Testcontainers** (MySQL, PostgreSQL, MongoDB), Awaitility, WireMock, ArchUnit, JaCoCo. Contract tests between services via Spring Cloud Contract or Pact (see D7).

---

## 4. Repository layout

```
vetsync-poc/
├── README.md                     ← this file
├── AGENTS.md                     ← instructions for coding agents
├── CONTRIBUTING.md
├── SECURITY.md
├── pom.xml                       ← Maven reactor and shared dependency management
├── services/
│   ├── pms-api/                  ← REST API and MySQL-facing adapters
│   ├── sync-service/             ← polling worker and event publisher
│   └── action-engine/            ← event consumer, rules, and dispatch adapters
└── .github/
    ├── copilot-instructions.md
    ├── agents/                   ← planner, implementer, read-only reviewer
    ├── instructions/             ← path-specific Java, workflow, and Markdown guidance
    ├── prompts/                  ← reusable task prompts
    ├── skills/                   ← repeatable development workflows
    ├── ISSUE_TEMPLATE/
    ├── PULL_REQUEST_TEMPLATE.md
    └── workflows/                ← one CI/CD workflow per service
```

Base package: `com.vetsync.<service>` (rename to your organisation's domain). The three Spring Boot applications are separate Maven modules and can be built independently with `mvn -pl services/<module> -am verify`.

Each service has its own hexagonal boundary and deployable container image:

```
services/pms-api/src/main/java/com/vetsync/pms/
├── domain/                  ← PMS entities and business rules
├── application/             ← use cases and ports
├── adapter/in/web/
│   ├── controller/          ← one controller per resource
│   └── dto/                 ← request/response DTOs grouped by resource
├── adapter/out/persistence/ ← MySQL adapters
└── config/                  ← Spring wiring

services/sync-service/src/main/java/com/vetsync/sync/
├── domain/                  ← stream and synchronization rules
├── application/             ← poll/sync use cases and ports
├── adapter/in/scheduler/    ← timer and job entry points
├── adapter/out/mysql/       ← read-only PMS source adapter
├── adapter/out/nosql/       ← progress and deduplication state
├── adapter/out/messaging/   ← event publisher
└── config/

services/action-engine/src/main/java/com/vetsync/action/
├── domain/                  ← action-engine-owned model and rules
├── application/             ← event handling and dispatch use cases
├── adapter/in/messaging/    ← event consumer and contract mapping
├── adapter/out/persistence/ ← PostgreSQL adapters
├── adapter/out/channel/     ← email, SMS, and staff-task channels
└── config/
```

Each workflow builds and tests only its service module (including required Maven modules), then offers an opt-in UAT deployment for a pre-provisioned Azure Container App. Deployment is available only when manually dispatching a workflow with `deploy_uat` enabled. Configure `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, and `AZURE_SUBSCRIPTION_ID` as `uat` environment secrets, and `AZURE_RESOURCE_GROUP`, `AZURE_ACR_NAME`, `AZURE_ACR_LOGIN_SERVER`, and `AZURE_CONTAINER_APP_NAME` as `uat` environment variables. The GitHub OIDC identity must have permission to push to ACR and update the target Container App.

---

## 5. Environments and configuration

| Env | Where it runs | Databases | Messaging | Purpose |
|-----|---------------|-----------|-----------|---------|
| `local` | Docker Compose on Windows 11 / Ubuntu | Containers | Emulator / RabbitMQ | Development, TDD |
| `uat` | Azure (own resource group) | Managed services, small SKUs, synthetic data | Service Bus | Integration and acceptance |
| `prod` | Azure (own resource group) | Managed services, HA SKUs | Service Bus | Live |

Rules:
- One Spring profile per environment: `application-local.yml`, `application-uat.yml`, `application-prod.yml`. Shared defaults in `application.yml`.
- **Every environment-specific value comes from environment variables** (`${MYSQL_URL}`, etc.). No hardcoded hostnames.
- In Azure, secrets are Key Vault references and services authenticate with managed identity where the service supports it.
- Local secrets live in `.env` (git-ignored). Commit `.env.example` only.
- Tests use the `test` profile plus Testcontainers. They must not depend on `local` Docker Compose being up.

---

## 6. Running locally (Windows 11 and Ubuntu)

**Prerequisites**
- JDK 21, Git
- Docker: **Docker Desktop with WSL2 backend** on Windows 11, Docker Engine on Ubuntu (Testcontainers needs a working Docker daemon on both)
- Maven 3.9.9 (until the Maven Wrapper is added in Phase 0).

**Commands (same on both OSes, apart from the wrapper name)**

```bash
docker compose -f infra/docker/docker-compose.yml up -d     # MySQL, Postgres, Mongo, messaging, Mailpit
mvn clean verify                                         # all unit + integration tests
mvn -pl services/pms-api spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl services/sync-service spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl services/action-engine spring-boot:run -Dspring-boot.run.profiles=local
```

Local-only database credentials are declared in Compose for convenience; do not reuse them outside development. Stop the dependencies with `docker compose -f infra/docker/docker-compose.yml down` (add `-v` to remove their local data).

**Local infrastructure (docker-compose)**

| Service | Port | Purpose |
|---------|------|---------|
| MySQL 8.4 | 3306 | One shared schema, created by Flyway |
| PostgreSQL | 5432 | Action engine DB |
| MongoDB | 27017 | Sync state |
| RabbitMQ | 5672 (AMQP), 15672 (management UI) | Local messaging adapter |
| Mailpit | 8025 (UI), 1025 (SMTP) | Catches outbound emails from the dispatcher |
| `pms-api` | 8081 | REST API; workers have no HTTP listener in the initial scaffold |

### Azure foundation (Terraform)

`infra/terraform/main.tf` provisions a development resource group, private networking for Container Apps and MySQL, an Azure Container Registry, Container Apps environment, MySQL Flexible Server, Service Bus queue, Key Vault, Log Analytics, and Application Insights. It does not deploy application containers. Cosmos DB and PostgreSQL are not provisioned yet because their use is still undecided or not implemented in the current services.

From `infra/terraform`, run `terraform init` and `terraform plan`; `terraform apply` provisions billable Azure resources. Terraform uses the active Azure CLI identity and prompts for the required MySQL administrator password. State files can contain sensitive values, so keep the local state private and configure a secured remote backend before collaborating or using this for production.

**Cross-platform rules for the agent and humans:**
- No bash-only or PowerShell-only scripts in the critical path. Use Maven, Docker Compose and Java.
- If a script is unavoidable, provide both `.sh` and `.ps1`.
- Use `Path`/`Paths` in code, never hardcoded `/` or `\`.
- Keep line endings LF (`.gitattributes`).

---

## 7. App 1: Source System (Core PMS)

### 7.1 Multi-tenancy model

Six tenants, one per Bulgarian planning region, share **one MySQL schema**. Every PMS business row and audit row carries a `tenant_id` (`SW`, `SE`, `SC`, `NE`, `NW`, or `NC`); tenant-scoped unique keys and composite foreign keys keep tenant-owned records separate. The PMS database adapter, Flyway schema, and region-to-tenant API resolution are implemented in `services/pms-api`; sync polling remains future work.

The PMS datasource is configured with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; these are required at runtime and have no checked-in defaults.

| Tenant code | Region |
|-------------|--------|
| `SW` | South West |
| `SE` | South East |
| `SC` | South Central |
| `NE` | North East |
| `NW` | North West |
| `NC` | North Central |

### 7.2 Shared-schema business tables

Flyway migrations under `services/pms-api/src/main/resources/db/migration` are the schema source of truth; Hibernate schema auto-DDL is disabled. The migration defines the 14 business tables sketched below in the shared schema; each has `tenant_id` and the common business metadata.

Conventions for every business table:
- `id BIGINT AUTO_INCREMENT` primary key + `public_id CHAR(36)` UUID (unique per tenant). The `public_id` is the cross-system identifier.
- `created_at`, `updated_at` (`TIMESTAMP(6)`), `version BIGINT` (optimistic locking, also used as the source version in sync events).
- **Soft delete** (`deleted_at`) so deletions are visible to the sync layer.
- Money as `DECIMAL(12,2)`, currency `EUR` (Bulgaria joined the euro area on 1 Jan 2026; keep the currency as a column anyway).

```mermaid
erDiagram
  CLINIC ||--o{ STAFF : employs
  CLINIC ||--o{ CLIENT : "registered at"
  CLIENT ||--o{ PATIENT : owns
  SPECIES ||--o{ BREED : has
  PATIENT }o--|| SPECIES : is
  PATIENT }o--o| BREED : is
  PATIENT ||--o{ VISIT : has
  STAFF ||--o{ VISIT : "attends (vet)"
  VISIT ||--o{ MEDICAL_RECORD_ENTRY : produces
  PATIENT ||--o{ MEDICAL_RECORD_ENTRY : history
  VISIT ||--o{ PRESCRIPTION : issues
  PRESCRIPTION ||--|{ PRESCRIPTION_ITEM : contains
  MEDICATION ||--o{ PRESCRIPTION_ITEM : "is prescribed"
  VISIT ||--o{ INVOICE : "billed by"
  CLIENT ||--o{ INVOICE : "pays"
  INVOICE ||--|{ INVOICE_LINE : contains
  INVOICE ||--o{ PAYMENT : "settled by"
```

**Business tables**

| Table | Key columns |
|-------|-------------|
| `clinic` | name, city, address, phone, email, active |
| `staff` | clinic_id, role (`VET`, `NURSE`, `RECEPTION`, `ADMIN`), full_name, license_no, active |
| `client` (owner) | registered_clinic_id, first_name, last_name, email, phone, address, city, preferred_contact, gdpr_consent_at |
| `species` / `breed` | code, name; breed.species_id |
| `patient` (pet) | client_id, clinic_id, name, species_id, breed_id, sex, birth_date, microchip_no (unique per tenant), neutered, weight_kg, status (`ACTIVE`, `DECEASED`, `TRANSFERRED`), deceased_at |
| `visit` | patient_id, clinic_id, vet_id, visit_type, status (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), scheduled_at, started_at, completed_at, reason, diagnosis, notes |
| `medical_record_entry` | patient_id, visit_id (nullable), entry_type (`DIAGNOSIS`, `VACCINATION`, `ALLERGY`, `PROCEDURE`, `NOTE`), description, recorded_at, recorded_by |
| `medication` | code, name, form, strength, controlled (bool). Small synthetic catalogue |
| `prescription` | patient_id, visit_id, vet_id, status (`ACTIVE`, `EXPIRED`, `CANCELLED`, `FULFILLED`), issued_on, valid_until, notes |
| `prescription_item` | prescription_id, medication_id, dosage, frequency, duration_days, quantity, refills_allowed |
| `invoice` | clinic_id, client_id, visit_id, invoice_no (unique per clinic and tenant), status (`DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `VOID`), issued_on, due_on, currency, total_amount, paid_amount |
| `invoice_line` | invoice_id, description, quantity, unit_price, vat_rate |
| `payment` | invoice_id, paid_at, amount, method (`CASH`, `CARD`, `BANK_TRANSFER`) |

### 7.3 Audit tables (the sync layer's input)

Separate audit tables per stream, so the sync service can poll each independently and at its own frequency:

| Audit table | Captures changes to | Consumed by stream |
|-------------|--------------------|--------------------|
| `audit_prescription` | prescription, prescription_item | prescriptions |
| `audit_visit` | visit, medical_record_entry | visits / medical status |
| `audit_invoice` | invoice, invoice_line, payment | invoices |
| `audit_master_data` | clinic, client, patient | master-data events |

The four MySQL audit tables and write triggers are implemented by Flyway. Each table adds `tenant_id` to this common shape:

```sql
audit_id      BIGINT AUTO_INCREMENT PRIMARY KEY,   -- "added order"
entity_type   VARCHAR(40)  NOT NULL,
entity_id     BIGINT       NOT NULL,
public_id     CHAR(36)     NOT NULL,
operation     ENUM('INSERT','UPDATE','DELETE') NOT NULL,
row_version   BIGINT       NOT NULL,
changed_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
changed_by    VARCHAR(100) NULL,
payload       JSON         NOT NULL,               -- snapshot of the row after the change
INDEX idx_changed_at (changed_at)
```

**How rows get written:** MySQL `AFTER INSERT` and `AFTER UPDATE` triggers capture writes, including writes that bypass the API. A first soft delete (`deleted_at` changing from `NULL` to a timestamp) is emitted as `DELETE`; later updates are emitted as `UPDATE`. The row snapshot includes tenant and business columns, and the triggers use the connection session variable `@pms_changed_by` when set. The PMS API writes business tables through tenant-scoped repositories; the sync poller is future work. Audit tables are not exposed as API write endpoints.

> ⚠️ **Ordering caveat, read this before writing the poller.**
> `AUTO_INCREMENT` ids are assigned at insert time, not commit time. Transaction A can get `audit_id=100` and commit *after* transaction B got `101` and committed. A poller that reads "everything > 100" right after B commits will **permanently skip row 100**.
> Mitigation built into this design: the poller only reads rows with `changed_at < now() - safetyLag` (default 5 s), and re-reads a small overlap window each run. Dedupe (section 8.4) makes the overlap harmless. Write a test that reproduces this race before implementing the poller.

> ⚠️ **Not everything is a row change.**
> "Prescription expired" and "invoice overdue / unpaid for > N days" happen because **time passes**, not because a row is written, so no audit row is produced. The sync service therefore needs two kinds of streams (section 8.2): *change-log streams* and *time-window scans* against the business tables.

### 7.4 REST API (Spring Boot)

Base path: `/api/v1/{region}` where `{region}` is one of the lowercase codes `sw|se|sc|ne|nw|nc`, mapped to tenant codes `SW|SE|SC|NE|NW|NC`. APIs use JSON DTOs, request validation, `201 Created` with `Location` for creates, `404` for missing or wrong-tenant resources/parents, and `409` for database constraint conflicts. Validation errors return `400`. `DELETE` soft-deletes business rows.

| Resource | Operations |
|----------|-----------|
| `/clinics`, `/staff`, `/clients`, `/patients`, `/visits`, `/prescriptions`, `/invoices` | list, get, create, update, soft-delete |
| `/clinics/{clinicId}/staff` | list and create staff for a clinic |
| `/clients/{clientId}/patients` | list and create patients for a client |
| `/patients/{patientId}/history` | list/add medical record entries; nested entry GET |
| `/patients/{patientId}/visits` | list and create patient visits |
| `/visits/{visitId}/prescriptions` | list and create prescriptions for a visit |
| `/invoices/{invoiceId}/payments` | list/register payments; nested payment GET |

Client listing supports a simple `q` match over name, phone, or email; patient listing supports exact `microchip`; invoice listing supports `status`. Nested creation verifies that referenced parents and related records belong to the requested tenant and match the path parent. Payment registration checks the invoice balance and updates its paid amount/status.

Authentication, OpenAPI documentation, ETag/If-Match optimistic concurrency, clinic transfer workflows, visit state-transition actions, invoice issue/void actions, species/breed/medication catalog endpoints, and prescription-item management are not implemented by this API yet.

The current API validates tenant/parent ownership, required fields and enum values, prescription date order, invoice paid totals, and payment balance. More involved workflow rules—such as immutable completed visits, medication catalogue/item management, and controlled-medication licensing—remain future work.

**Seed data:** a Java-based synthetic data generator (e.g. Datafaker), profile `local` only, configurable volume (e.g. 50 clinics, 100k clients, 150k pets across regions) to make "big database" tests realistic.

---

## 8. App 2: Middle Sync Service

### 8.1 Responsibilities
- **Initial sync:** full snapshot per tenant and entity, chunked with keyset pagination (`WHERE id > :last ORDER BY id LIMIT :n`), resumable after a crash.
- **Partial sync:** re-sync by tenant, by entity type, by id, or by time range on demand (admin endpoint or CLI argument).
- **Timer-driven polling** of `audit_prescription`, `audit_visit`, `audit_invoice`.
- **Event listening** for client / clinic / patient changes (near-real-time, from `audit_master_data`).
- **Time-window scans:** prescriptions expiring within N days or already expired; unpaid invoices issued in the last 6 months.
- **Deduplication and progress tracking** in the NoSQL store.
- Publish a versioned `SyncEnvelope` to the messaging layer.

### 8.2 Stream types

| Stream | Type | Source | Trigger |
|--------|------|--------|---------|
| `master-data` | change-log | `audit_master_data` | Short interval (long-running listener, e.g. every 2–5 s) |
| `visits` | change-log | `audit_visit` | Timer job |
| `prescriptions` | change-log | `audit_prescription` | Timer job |
| `invoices` | change-log | `audit_invoice` | Timer job |
| `prescription-expiry` | time-window scan | `prescription` where status=ACTIVE and `valid_until` ≤ today + N | Timer job (daily or hourly) |
| `unpaid-invoices` | time-window scan | `invoice` where status ∈ {ISSUED, PARTIALLY_PAID} and `issued_on` ≥ today − 6 months | Timer job |

Each stream runs **per tenant** (6 × streams pointers).

### 8.3 Run modes

The same Spring Boot image supports two modes, selected by configuration:

- `sync.mode=worker`: long-running process with `@Scheduled` pollers (local dev, master-data listener).
- `sync.mode=job` (`--sync.stream=invoices --sync.tenant=ALL`): run **one polling pass and exit**. This is what an Azure Container Apps Job (cron-triggered) executes.

The polling logic itself is one class, `PollStreamUseCase`, called by both modes, so it is tested once.

### 8.4 NoSQL state model

Collections (or containers, when on Cosmos):

```jsonc
// pointers: one doc per (tenant, stream)
{ "_id": "SW:invoices", "lastAuditId": 18422, "lastChangedAt": "2026-09-30T08:15:03Z",
  "lastRunAt": "...", "status": "IDLE|RUNNING|FAILED", "lastError": null }

// leases: prevents two instances polling the same (tenant, stream)
{ "_id": "SW:invoices", "owner": "instance-id", "expiresAt": "..." }   // TTL-indexed

// processed: deduplication
{ "_id": "SW:invoice:9912:v7", "processedAt": "...", "expiresAt": "..." } // TTL, e.g. 30 days

// initial-sync progress
{ "_id": "SW:patient", "lastId": 45000, "state": "IN_PROGRESS|DONE", "startedAt": "..." }
```

Dedupe key = `tenant + entityType + publicId + rowVersion` (for time-window scans, add the scan date and rule name, e.g. `...:prescription-expiry:2026-09-30`).

### 8.5 Event contract (`libs/sync-contracts`)

```jsonc
{
  "schemaVersion": 1,
  "eventId": "uuid",
  "correlationId": "uuid",
  "tenant": "SW",
  "stream": "invoices",
  "entityType": "INVOICE",
  "entityId": "public-uuid",
  "operation": "UPSERT | DELETE | EXPIRY_DUE | OVERDUE",
  "sourceVersion": 7,
  "occurredAt": "2026-09-30T08:15:03Z",
  "publishedAt": "2026-09-30T08:15:09Z",
  "payload": { /* entity snapshot: DTO specific to entityType */ }
}
```

- Payload DTOs live in `sync-contracts` with JSON schema; **breaking changes bump `schemaVersion`**.
- Publishing is **at-least-once**; the consumer must be idempotent.
- Ordering: use the Service Bus **session id** = `tenant:aggregateRootId` (e.g. patient) if per-aggregate ordering matters (see D4).

### 8.6 Failure handling
- Advance the pointer **only after** the broker confirms publish.
- Retries with backoff (Resilience4j); poison events → dead-letter queue + alert.
- Admin endpoints (secured): `POST /admin/sync/initial`, `POST /admin/sync/partial`, `GET /admin/sync/status`.

---

## 9. App 3: Action Engine & Dispatcher

### 9.1 Responsibilities
1. **Consume** `SyncEnvelope` events.
2. **Translate** them through an anti-corruption layer into the engine's **own domain model** (different names, different shape; it must not mirror the PMS schema).
3. **Decide** which actions are due, using rules.
4. **Dispatch** actions through channel adapters.
5. Record everything for audit and idempotency.

### 9.2 Own domain (proposal)

| PMS concept | Action Engine concept |
|-------------|-----------------------|
| Client | `Contact` (name, channels, consent, preferred language) |
| Patient | `CareSubject` (species, age band, owner link, status) |
| Prescription | `Treatment` (`activeUntil`, items summary) |
| Visit | `CareEvent` (type, occurredAt, outcome) |
| Invoice | `Receivable` (amount due, due date, age) |
| Clinic | `Site` |

### 9.3 Example rules (first slice)
- **Treatment expiring:** treatment ends in ≤ 7 days and no renewal exists → create `REMINDER` action to the contact.
- **Overdue receivable:** unpaid > 30 days → `PAYMENT_REMINDER`; > 60 days → escalate to clinic staff. (Thresholds configurable.)
- **Post-visit follow-up:** completed visit of type `SURGERY` → follow-up check after 3 days.

### 9.4 Dispatcher
- Port `ActionChannel` with adapters: `EmailChannel` (SMTP → Mailpit locally), `SmsChannel` (stub / log), `StaffTaskChannel` (writes a task row).
- Actions have states `PLANNED → DISPATCHED → CONFIRMED | FAILED` with retry and a max-attempts cap.
- **Inbox table** (`processed_event(event_id PK)`) for consumer idempotency. Actions have a natural unique key (e.g. `rule + subjectId + windowStart`) so redelivery cannot create duplicates.

### 9.5 PostgreSQL (initial tables)
`contact`, `care_subject`, `treatment`, `care_event`, `receivable`, `site`, `action`, `action_attempt`, `processed_event`. Flyway migrations.

---

## 10. Testing strategy (TDD)

**The loop:** write one failing test → make it pass with the simplest code → refactor → commit. Small commits, tests first in the diff.

| Level | Tooling | Scope |
|-------|---------|-------|
| Unit | JUnit 5, AssertJ, Mockito | Domain rules, use cases with fake ports. No Spring context, so it runs in milliseconds. |
| Slice/adapter | `@WebMvcTest`, `@DataJpaTest`/JDBC with **Testcontainers** | Controllers, repositories, Flyway migrations against real MySQL/Postgres/Mongo |
| Contract | Spring Cloud Contract or Pact | `sync-service` ↔ `action-engine` event schema; guards `sync-contracts` |
| Integration | Testcontainers + Awaitility | Poller against real MySQL with audit rows (including the out-of-order commit race), consumer idempotency |
| Architecture | ArchUnit | Hexagonal rules, no PMS imports in action-engine |
| E2E (few) | Compose or Testcontainers network | Create invoice in PMS → event arrives → action planned |

Rules:
- No code without a failing test first. Bug fix = failing regression test first.
- Tests must be deterministic: inject a `Clock`, never call `Instant.now()` in domain code.
- Tests must pass on Windows 11 and Ubuntu (no OS-specific paths or shell calls).
- Coverage gate in CI (JaCoCo, start at 80% line coverage on domain + application layers).
- Naming: `MethodOrBehavior_ShouldExpectedResult_WhenCondition` or a consistent BDD style, whichever you choose, keep it consistent.

---

## 11. Azure deployment

Per environment (`uat`, `prod`), one resource group:

| Resource | Used for |
|----------|----------|
| Azure Container Registry | Images for the three apps |
| Container Apps Environment | Hosts `pms-api`, `sync-service` (worker), `action-engine` |
| Container Apps **Jobs** (cron) | `sync-service` in `job` mode, one job per stream |
| Azure Database for MySQL – Flexible Server | PMS (6 schemas) |
| Azure Database for PostgreSQL – Flexible Server | Action engine |
| Azure Cosmos DB (MongoDB API) | Sync state (see D3) |
| Azure Service Bus | Events between sync and action engine |
| Key Vault + managed identities | Secrets and access |
| Application Insights + Log Analytics | Logs, traces, metrics, alerts |

Pipeline target (GitHub Actions): `build & test` → `build image` → `push to ACR` → `deploy UAT` → `smoke tests` → `manual approval` → `deploy PROD`. The current per-service workflows implement build/test and an opt-in UAT deployment to pre-provisioned resources; infra provisioning, smoke tests, production promotion, and OIDC configuration remain follow-up work.

---

## 12. Roadmap: start with the initial layer of each app

Each phase should end green (`mvn clean verify`) and merged. Start each task by writing the failing test named in *First tests*.

**Phase 0: Scaffold**
- Initial parent POM and three Spring Boot service modules, hexagonal package boundaries, per-service Dockerfiles and GitHub Actions workflows are in place.
- Coding-agent guidance, GitHub Copilot agents/prompts/skills, and contribution templates are in place.
- Remaining: `sync-contracts`, Maven Wrapper, `.gitattributes`, docker-compose, and ArchUnit skeleton.
- *First tests:* each app's context loads (starter tests included); ArchUnit skeleton passes.

**Phase 1: PMS layer 1**
- **Implemented database layer:** Flyway migrations for the shared schema, all 14 business tables, four audit tables, tenant-scoped constraints, and audit/version triggers. MySQL Testcontainers integration tests cover schema creation, a Spring Data repository, and audit stream behavior.
- Remaining: URL-to-tenant resolution and API CRUD for `clinic`, `client`, and `patient`.
- *Next tests:* tenant-aware API requests never return another tenant's records; API writes are covered by the database audit triggers.
- *Done when:* the three CRUD resources work in all six regions and audit rows are verified.

**Phase 2: PMS layer 2**
- `visit`, `prescription`, `invoice`, payments + their audit tables + business rules + seed generator.

**Phase 3: Sync layer 1**
- `sync-contracts` v1, pointer + lease + dedupe repositories (Mongo Testcontainer), `PollStreamUseCase` for **`master-data` only**, publishing through a `EventPublisher` port (in-memory fake first).
- *First tests:* pointer advances only after publish success; duplicate audit row publishes once; **out-of-order commit is not skipped** (uses safety lag).
- *Done when:* creating a client in PMS produces a `SyncEnvelope` on the (fake, then real) broker within the configured lag.

**Phase 4: Action engine layer 1**
- Consumer + inbox idempotency + ACL mapping `Client → Contact`, `Patient → CareSubject`; Postgres schema.
- *First tests:* same event delivered twice creates one `Contact`; unknown `schemaVersion` is dead-lettered.

**Phase 5: Vertical slice: unpaid invoices**
- Time-window scan → event → `Receivable` → overdue rule → email in Mailpit.

**Phase 6: Prescriptions and visits streams, initial + partial sync, job mode.**

**Phase 7: Azure**: Bicep, UAT deployment, cron jobs, observability, then PROD.

---

## 13. Coding-agent working agreement (also in `AGENTS.md` and `.github/copilot-instructions.md`)

```markdown
# Rules for the agent
- Follow TDD strictly: write a failing test, show it failing, implement the minimum, run tests, refactor.
- Work on ONE roadmap phase task at a time (README section 12). Ask before starting a new phase.
- Build/test with Maven 3.9.9 until the Maven Wrapper is added: `mvn clean verify`.
- Everything must work on Windows 11 AND Ubuntu: no OS-specific paths or shell-only scripts.
- Respect module boundaries: action-engine must not import PMS classes; sync-service accesses MySQL read-only;
  once introduced, shared types live only in libs/sync-contracts.
- Never hardcode secrets or hostnames. Configuration comes from env vars / Spring profiles (local, uat, prod).
- Never use real personal data; synthetic data only.
- Domain code takes a java.time.Clock; do not call Instant.now() directly.
- Any change to the event contract requires a schemaVersion decision and a contract-test update.
- Record non-trivial architecture decisions as a short ADR in docs/adr/.
- If a requirement or a fact (library version, Azure feature) is uncertain, say so and ask; do not guess.
- Before finishing a task: run the full test suite and summarise what changed and what is still open.
```

GitHub Copilot custom agents, prompts, skills, and path-specific instructions live under `.github/`. See `CONTRIBUTING.md` for contributor workflow and `.github/PULL_REQUEST_TEMPLATE.md` for the pull request checklist. Azure provisioning and deployment require explicit approval; workflow configuration alone does not authorize a deployment.

Repository agent entry points:

| File or directory | Purpose |
|-------------------|---------|
| `AGENTS.md`, `.github/copilot-instructions.md` | Shared repository and Copilot rules |
| `.github/agents/` | Planner, implementer, and read-only reviewer roles |
| `.github/prompts/` | Reusable feature, review, sync-stream, and contract-change tasks |
| `.github/skills/` | Repeatable TDD, service-boundary, and GitHub Actions guidance |
| `.github/instructions/` | Java, workflow, and Markdown path-specific instructions |
| `.github/ISSUE_TEMPLATE/`, `.github/PULL_REQUEST_TEMPLATE.md` | Structured contribution intake and handoff |
| `CONTRIBUTING.md`, `SECURITY.md` | Contributor workflow and security reporting guidance |

---

## 14. Open decisions and things to verify (not assumed)

These are deliberately flagged rather than decided silently. Turn each into an ADR once chosen.

| ID | Question | Current proposal | Why it's open |
|----|----------|------------------|---------------|
| D1 | Tenancy: schema-per-region vs shared schema with `tenant_id` | **Resolved: one shared schema with `tenant_id`** | Chosen for this POC; tenant-scoped unique keys and composite foreign keys constrain business data. |
| D2 | Audit writing: DB triggers vs application-level | **Resolved: MySQL triggers** | Implemented in Flyway; triggers also advance row versions on direct SQL updates. |
| D3 | Sync state store | MongoDB local, Cosmos DB (Mongo API) in Azure | **Verify** Cosmos Mongo API supports the features used (TTL indexes, unique indexes, transactions you rely on) and behaves the same as local MongoDB in tests. Alternative: Cosmos NoSQL API with its emulator, or Azure Table Storage. |
| D4 | Messaging and local emulation | Azure Service Bus; local via the Service Bus emulator or RabbitMQ behind a port | **Verify** current emulator capabilities and limits (including whether it runs cleanly on both Windows/Docker Desktop and Ubuntu, and how topics/queues are configured). Also decide whether per-aggregate ordering (sessions) is required. |
| D5 | Timers: Container Apps Jobs vs Azure Functions timer trigger | Container Apps Jobs (cron), same image as worker | **Verify** the minimum practical schedule granularity (cron is typically per-minute, so "near real-time" below one minute needs the long-running listener instead). |
| D6 | Master-data events: poll `audit_master_data` frequently vs PMS publishes events directly | Frequent polling of the audit table | Direct publishing gives lower latency but needs an outbox in PMS. Polling is simpler for a POC. |
| D7 | Contract testing tool | Spring Cloud Contract or Pact | Pick one; both work, and the choice depends on team familiarity. |
| D8 | Spring Boot / Java versions | Java 21, Spring Boot 3.5.x+ | **Verify the latest stable Spring Boot** and compatibility with the libraries above at project start. |
| D9 | Prescription "expiry" and "overdue" semantics | Time-window scans (section 8.2) | Confirm business definitions (N days before expiry; overdue threshold; does "unpaid from last 6 months" use `issued_on` or `due_on`). |
| D10 | Delete semantics | Soft delete propagates as `DELETE` events | Confirm the Action Engine should remove or archive the corresponding records. |

**Known constraints to remember**
- Testcontainers requires Docker on both OSes (Docker Desktop + WSL2 on Windows 11).
- Audit polling by auto-increment id needs the safety-lag mitigation (section 7.3).
- Delivery is at-least-once end to end. Idempotency is a requirement, not an option.