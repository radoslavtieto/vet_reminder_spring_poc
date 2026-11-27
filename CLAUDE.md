# CLAUDE.md

@AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`AGENTS.md` holds the engineering rules (TDD, module boundaries, `Clock` injection, synthetic data, no Azure deployment without approval). Follow it. This file covers commands and the cross-service structure that `AGENTS.md` does not.

## Commands

Requires JDK 21. Run from the repo root. In Windows PowerShell use `.\mvnw.cmd` in place of `./mvnw`. The wrapper is present at the root (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`), although AGENTS.md says otherwise.

```sh
./mvnw -B clean verify                                   # full reactor
./mvnw -B -pl services/sync-service -am verify           # one service; -am also builds libs/sync-contracts
./mvnw -B -pl services/pms-api -am verify -Dapi.version=1.44
```

`-Dapi.version=1.44` is needed for Docker Engine 29 (see `services/pms-api/docs/testing.md`). Testcontainers tests need a running Docker daemon. They start their own containers, so the Compose services do not need to be up. Do not report a Docker-dependent test as passing if it did not run.

Single test. The `-Dsurefire.failIfNoSpecifiedTests=false` flag is required with `-am`, otherwise upstream modules fail with "No tests were executed":

```sh
./mvnw -B -pl services/sync-service -am test -Dtest=PollJobRunnerTests -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -B -pl services/action-engine -am test -Dtest='SyncEventProcessorTests#someMethod' -Dsurefire.failIfNoSpecifiedTests=false
```

No lint or formatter is configured (no Checkstyle, Spotless, or PMD). JaCoCo is wired into the POMs.

Local run. Each service has its own Compose file; there is no aggregate stack. Run `docker compose` from the repo root:

```sh
docker compose -f infra/pms-api/docker-compose.yml up -d mysql           # MySQL 8.4
docker compose -f infra/sync-service/docker-compose.yml up -d            # MongoDB 7 + RabbitMQ 4
docker compose -f infra/action-engine/docker-compose.yml up -d           # PostgreSQL 16 + Mailpit
./mvnw -pl services/pms-api spring-boot:run
./mvnw -f services/sync-service/pom.xml spring-boot:run
./mvnw -f services/action-engine/pom.xml spring-boot:run
```

Configuration is environment variables only, with no Spring profiles. Names are in each service's `src/main/resources/application.properties`. The poller and the listener are off by default. Enable them with `SYNC_POLL_ENABLED=true` and `ACTION_MESSAGING_ENABLED=true`.

## Architecture

**Pipeline:** `pms-api` writes PMS data to MySQL. MySQL triggers (not application code) write `audit_*` rows. `sync-service` polls those audit tables read-only, builds a v1 `SyncEnvelope`, and publishes it to RabbitMQ. `action-engine` consumes the events and persists its own records to PostgreSQL. The only implemented stream is `master-data` (clinic, client, patient).

**Layout.** Each service is a hexagonal layout under `com.vetsync.{pms,sync,action}`: `domain`, `application` (use cases plus ports, such as `EventPublisher`, `AuditChangeSource`, `ProcessedEventRepository`), `adapter/in` (REST, schedulers, RabbitMQ listener), `adapter/out` (MySQL, Mongo, RabbitMQ, JDBC), and `config`. Keep wiring in `config`, not in use cases.

**Shared contract.** `libs/sync-contracts` holds `SyncEnvelope`, `SyncOperation`, and `sync-envelope-v1.schema.json`. Both `sync-service` and `action-engine` depend on it, and neither may import PMS classes. A breaking change requires a `schemaVersion` bump and an update to the contract tests.

**Multi-tenancy.** `pms-api` uses one shared MySQL schema. Every business row has a `tenant_id`, and the six tenants are `SW, SE, SC, NE, NW, NC`. The tenant comes from the JWT via `TenantResolver`. Ownership and authorization are checked server-side in `application/Pms*ApplicationService`.

**Sync flow** (`PollMasterDataUseCase`). Read a bounded batch from `AuditChangeSource` (`JdbcAuditChangeSource`). Use a 5 s safety lag and a 10 s overlap re-read. Publish through `RetryingEventPublisher`. Advance the MongoDB pointer (`sync_pointers`, via `PointerRepository`) only after the publisher confirms. Schedulers in `adapter/in/scheduler` drive this.

**Action flow.** `RabbitSyncEventListener` → `SyncEventProcessor` → `SyncEnvelopeMapper`. The mapper turns CLINIC into `Site`, CLIENT into `Contact`, and PATIENT into `CareSubject`. `JdbcActionDataStore` writes the inbox claim and the domain record in one transaction. Source-version checks reject stale updates, and soft deletes set `active=false`.

**Delivery semantics.** Delivery is at-least-once end to end. `eventId` is deterministic (tenant, entity type, public ID, source version), so retries produce the same ID. Consumers must be idempotent. The `processed_event` inbox in PostgreSQL provides that.

## Gotchas

- **README status is partly aspirational.** Before assuming a feature exists, check the code. README §8.7 and §9.6 list what is implemented.
- **An Azure Service Bus publisher exists in code.** `ServiceBusEventPublisher` is enabled by `sync.messaging.service-bus.enabled`. AGENTS.md and the README still describe the production adapter as open. Treat it as unverified against real Azure.
- **Audit coverage is incomplete.** PMS triggers do not emit audit rows for `staff`, `species`, `breed`, or `medication`. Snapshot code knows about those tables, but changes to them do not propagate continuously.
- **Not implemented:** the other audit streams, leases, sync-run records, time-window scans, dead-letter replay, and action rules and dispatch. Do not build on them as if they existed.
- **Decisions are open.** README §14 lists them (D3–D9). Ask before resolving them. Record decisions in `docs/adr/`.
- `.github/instructions/`, `.github/skills/`, and `.github/copilot-instructions.md` mirror the rules in AGENTS.md. They add no new constraints, so AGENTS.md is the authority.
