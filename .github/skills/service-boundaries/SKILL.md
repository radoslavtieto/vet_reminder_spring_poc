---
name: service-boundaries
description: Use when adding or changing behavior that touches more than one VetSync service or data store.
---

# VetSync service boundaries

- `pms-api` owns PMS domain data and writes to MySQL.
- `sync-service` reads PMS data/audit streams only; it owns synchronization pointers, leases, deduplication state, and event publication.
- `action-engine` consumes published events and owns its independent domain and persistence. It must not access PMS MySQL or import PMS classes.
- Event contracts belong in a dedicated shared contract module when that module exists; do not duplicate producer/consumer DTOs or introduce a guessed schema.
- Treat delivery as retryable and at-least-once where applicable. Consumers must be idempotent; source progress advances only after publication succeeds.
- Prefer explicit ports and service-owned adapters. Do not share database entities between services.
- Check the README open decisions before choosing tenancy, audit, storage, messaging, timer, or event semantics. Ask the user if the choice is consequential and remains undecided.

For cross-service changes, identify every producer, consumer, persisted state, rollout order, compatibility effect, and test needed before editing. Verify every affected service.
