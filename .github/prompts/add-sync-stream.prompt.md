---
mode: agent
description: Add or extend a synchronization stream while protecting cursor safety and idempotency.
---

Request: ${input:request:Describe the stream or synchronization behavior}

Inspect the actual sync-service implementation and existing tests first. Do not invent a shared event schema: the `sync-contracts` module is not implemented yet. Keep the PMS source access read-only and use the existing application/adapter boundaries.

Before implementing, determine whether the request depends on unresolved decisions in README sections 8 and 14. Ask the user if a choice materially affects event meaning, ordering, scheduling, or persistence. Add tests for retries, duplicate input, publish failure, cursor advancement, and the audit ordering/safety-lag behavior that applies to the change. Never advance a pointer before confirmed publication. Run the sync-service Maven verification and report any missing infrastructure or decisions.
