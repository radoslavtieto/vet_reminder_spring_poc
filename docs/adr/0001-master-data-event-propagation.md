# ADR 0001: Master-data event propagation

- Status: Accepted for the implemented POC slice
- Scope: `audit_master_data` changes for clinics, clients, and patients

## Decision

- Keep producer/consumer event types in the non-deployable `libs/sync-contracts` module. The first envelope version is `schemaVersion: 1`.
- Poll the PMS audit table read-only from `sync-service`, using a safety lag and overlapping lookback. Persist the per-tenant stream pointer in MongoDB only after RabbitMQ confirms publication.
- Use RabbitMQ behind application ports for local propagation. Azure Service Bus remains the intended Azure option, but its adapter, topology, and emulator are not selected or implemented.
- Generate a stable event ID from tenant, entity type, public ID, and source version. Treat delivery as at-least-once.
- In `action-engine`, map source snapshots into `Site`, `Contact`, and `CareSubject`; persist an inbox claim and source-version-guarded upsert in one PostgreSQL transaction.
- Convert soft-delete events into `active=false`; do not physically remove the Action Engine record in this slice.

## Consequences

- A crash after publish but before pointer update can produce a duplicate, which the Action Engine inbox safely ignores.
- RabbitMQ does not guarantee per-aggregate ordering. Source-version checks prevent stale updates from replacing newer records.
- Invalid/unhandled messages are rejected to the Action Engine dead-letter queue. Automated replay, alerting, and production broker verification remain follow-up work.
- Initial/partial snapshots, other audit streams, time-window scans, sync leases/dedupe, action rules, and dispatch are not included.
