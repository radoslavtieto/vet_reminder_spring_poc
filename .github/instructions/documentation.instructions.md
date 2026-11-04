---
applyTo: "**/*.md"
---

# Documentation

- Distinguish implemented behavior from proposals and roadmap items.
- Keep commands and file paths consistent with the checked-in project. `libs/sync-contracts`, the Maven Wrapper, and service-local Compose files exist; there is no aggregate Compose stack. Terraform is an infrastructure scaffold, not evidence that Azure resources or app deployments exist.
- Do not add real personal/clinical data, secrets, or identifiers for actual cloud resources.
- When documenting an architecture or contract change, include compatibility, ownership, and operational consequences.
- Preserve existing terminology for `pms-api`, `sync-service`, `action-engine`, tenants, streams, and events.
