# PMS API tests

This guide describes the existing PMS API tests and the current gaps visible from the test suite. Tests use synthetic fixtures only. The Testcontainers tests start their own MySQL 8.4 containers; the service's local Compose stack does not need to be running.

## Run and display tests

From the repository root, run all PMS API tests and build the required Maven modules:

**Windows PowerShell**

```powershell
.\mvnw.cmd -B '-Dapi.version=1.44' -pl services/pms-api -am test
```

**Bash or Git Bash**

```bash
./mvnw -B -Dapi.version=1.44 -pl services/pms-api -am test
```

`-Dapi.version=1.44` selects the Docker API version used by the workspace test configuration. `-am` also builds required reactor modules. Docker must be running for tests tagged `integration` or `e2e`.

- **VS Code:** Open **Testing** (the beaker icon), expand the Java test tree, then run a class, method, or the full test suite. The workspace Java test settings pass the Docker API version property.
- **IntelliJ IDEA:** Run a test, class, or package from the gutter or the Run window. For Testcontainers tests, set `-Dapi.version=1.44` in the JUnit run configuration's VM options.
- **Maven output:** Surefire prints the test totals and failures in the terminal. Detailed reports are written under `services/pms-api/target/surefire-reports/`.

To generate and open the JaCoCo HTML coverage report, run `verify` instead of `test`:

```powershell
.\mvnw.cmd -B '-Dapi.version=1.44' -pl services/pms-api -am verify
```

```bash
./mvnw -B -Dapi.version=1.44 -pl services/pms-api -am verify
```

After a successful run, open `services/pms-api/target/site/jacoco/index.html` in a browser. JaCoCo's Maven `check` also enforces at least 90% line coverage for `PmsApplicationService`. This is a class-specific threshold, not a claim that the entire API or every endpoint has 90% coverage.

The verification run for this documentation change measured 91.69% line coverage for `PmsApplicationService` (353 of 385 lines).

## Current coverage

The checked-in PMS API suite contains 55 JUnit tests across 11 test classes. The tests use Mockito and fixed clocks for isolated application behavior, Spring MVC tests for web/security behavior, and Testcontainers with MySQL for database-backed integration scenarios.

| Test class | Test style | What it covers | Why it matters |
|---|---|---|---|
| `PmsApplicationServiceBehaviorTests` | Unit, mocked repositories | Invoice amount and status rules, tenant resolution, soft-delete filtering and clock behavior | Protects business invariants without depending on MySQL |
| `PmsApplicationServiceCrudBehaviorTests` | Unit, mocked repositories | CRUD and relationship mapping for clinics, staff, clients, patients, visits, prescriptions, history, catalog, invoices and payments; tenant scope; visit transitions; patient transfers; controlled-medication rules | Exercises application rules and cross-record ownership checks |
| `PmsAuthenticationServiceTests` | Unit, mocked collaborators | Username normalization, generic login failures, disabled users, password hashing, staff-role requirements, JWT claims and token lifetime bounds | Guards account creation and authentication behavior |
| `PmsAdminBootstrapTests` | Unit, mocked collaborators | Configured initial admin creation, password strength, hashing, and no-op behavior when credentials are absent | Prevents unsafe or unusable bootstrap accounts |
| `PmsSecurityWebTests` | Spring MVC slice | Public login, missing or insufficient role, tenant mismatch, and reception access to selected routes | Verifies selected HTTP security boundaries |
| `PmsApiValidationTests` | Spring MVC slice | Invalid request rejection before application calls, error response mapping, sanitized logs, and service identity configuration | Protects the HTTP boundary and avoids exposing rejected values or database details |
| `PmsApiIntegrationTests` | Spring Boot + MockMvc + MySQL Testcontainer | Region-scoped clinic and nested staff flow; linked client, patient, visit, prescription, invoice and payment flow; invalid and cross-tenant associations | Checks multi-layer behavior against the real relational schema |
| `PmsApiEndToEndTests` | Random-port HTTP + MySQL Testcontainer | Clinic creation and retrieval over HTTP, including a cross-tenant read denial | Checks the running HTTP stack, not only MockMvc |
| `PmsDatabaseIntegrationTests` | Spring Boot + MySQL Testcontainer | Flyway tables, repository persistence, tenant constraint behavior, and insert/update/delete audit triggers and stream routing | Verifies database-specific migrations and change capture |
| `PmsLocalSeedIntegrationTests` | MySQL Testcontainer | Local seed leaves non-empty databases unchanged and is idempotent on an empty migrated database | Keeps local setup repeatable and prevents overwriting existing data |
| `OpenApiDocumentationTests` | Spring Boot + MockMvc + MySQL Testcontainer | OpenAPI document, selected route/security metadata, and Swagger UI | Ensures the published API description remains available |

## What should be covered next

Prioritize these additions as API behavior or security changes are made:

1. **Complete HTTP contract coverage by resource.** Add focused request/response tests for currently unexercised controller operations and edge cases, including staff, clients, patients, visits, prescriptions, invoice actions, catalog lookups, and user management. Check status codes, response fields, `Location` headers, and not-found/conflict behavior.
2. **Complete the authorization matrix.** Test anonymous, each role, and cross-tenant access for every route family and HTTP method, including JWT expiry, wrong issuer, malformed tokens, and tenant claims that disagree with the region in the URL. Existing web tests cover only selected routes and roles.
3. **Prove transactional and concurrent persistence behavior.** Add database-backed tests for rollback on failed multi-row operations and concurrent payments at the invoice balance boundary, so transactions cannot produce overpayment or partial updates.
4. **Broaden database constraint and audit checks.** Exercise tenant-scoped unique keys and foreign-key rules across entity relationships, and confirm audit payloads and delete/update behavior for each configured audit stream, not just representative records.
5. **Validate every input boundary and error path.** Cover request size/range/enum constraints and consistent safe error responses for all controllers, including that sensitive request values and SQL details never appear in logs or responses.
6. **Keep API documentation aligned.** Check the OpenAPI paths, request/response schemas, and security requirements for all routes rather than only the clinic endpoint.

The suite does not currently establish performance, load, or production-environment behavior. Those require separate, explicitly scoped validation and should not be inferred from unit or Testcontainers results.
