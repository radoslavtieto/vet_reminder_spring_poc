# Repository agent guide

## Project shape

This repository contains three independently deployable Java 21 / Spring Boot 3.5.6 services:

- `services/pms-api`: PMS REST API; owns its MySQL-facing domain and writes.
- `services/sync-service`: reads PMS data and audit streams, tracks synchronization state, and publishes events.
- `services/action-engine`: consumes published events and owns its domain, persistence, rules, and dispatch.

The root `pom.xml` is the Maven reactor and includes the non-deployable `libs/sync-contracts` module. Service-local Compose files provide MySQL, MongoDB, RabbitMQ, PostgreSQL, and Mailpit; there is no aggregate local stack. The Maven Wrapper is not present. Terraform provides an Azure infrastructure scaffold but does not deploy the application containers. Local RabbitMQ is implemented; the production Azure Service Bus adapter remains open.

## Engineering rules

- Make focused changes that preserve the service boundaries. The sync service accesses PMS data read-only; the action engine must not access MySQL or depend on PMS classes. Shared event types belong in `libs/sync-contracts`.
- Follow test-first development for behavior changes: add a failing test, implement the smallest correct change, then refactor.
- Keep business rules in domain/application code, not controllers or persistence adapters. Prefer ports and adapters at integration boundaries.
- Inject `java.time.Clock` wherever domain behavior depends on current time. Do not call `Instant.now()` in domain code.
- Keep APIs and event handling idempotent where delivery can be retried. Never advance sync progress before successful event publication.
- Use synthetic data only. Never add real personal, clinical, credential, or customer data to code, tests, fixtures, logs, or documentation.
- Do not hardcode secrets, environment-specific hostnames, or Azure resource identifiers. Use configuration and environment variables.
- Do not deploy or provision Azure resources as part of ordinary code changes. Azure access, production actions, and changes with external side effects require explicit user approval.
- Record non-trivial architecture choices in `docs/adr/` once that directory is established. Do not silently resolve decisions listed in README section 14.
- Update the README or relevant documentation when a change alters the architecture, developer workflow, or API/event contract.
- Avoid unrelated changes, especially to IDE files and existing user modifications.

## Build and test

Use JDK 21 and the checked-in Maven Wrapper, which downloads Maven 3.9.11. In Bash (including Git Bash), run:

```sh
./mvnw -B -pl services/pms-api -am verify
./mvnw -B -pl services/sync-service -am verify
./mvnw -B -pl services/action-engine -am verify
./mvnw -B clean verify
```

In Windows PowerShell, use `.\mvnw.cmd` in place of `./mvnw`. GitHub Actions workflows install Maven 3.9.9. Use the narrowest verification command that covers the change. Testcontainers integration tests require a running Docker daemon; they do not depend on the local Compose services being up. Do not report a test as passing if it was skipped or could not run.

## Before handing off

- Review the complete diff and ensure no unrelated files or generated build output were added.
- Run the relevant tests and report the exact outcome, including blockers.
- State any remaining assumptions or follow-up work; do not imply the scaffold implements unbuilt roadmap features.
