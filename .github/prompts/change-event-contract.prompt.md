---
mode: agent
description: Assess an event-contract change for compatibility and required versioning decisions.
---

Requested contract change: ${input:request:Describe the event change}

Inspect the actual producers, consumers, and contract tests before editing. The shared `sync-contracts` module is not yet present; do not create or assume one without making that scope explicit.

Determine whether the change is backward-compatible. Identify producer/consumer rollout order, old-message handling, schema-version consequences, and required tests. Do not silently choose a breaking-change policy when the answer affects compatibility; ask the user. If implementation is appropriate, make the smallest test-backed change, update relevant documentation, and verify each affected service.
