# PMS API diagrams

These diagrams describe the checked-in `pms-api` implementation and infrastructure scaffold:

- [PMS API architecture (SVG)](pms-api-architecture.svg) shows the web, security, application, persistence, MySQL, and audit-reader layers. Its Mermaid source is [pms-api-architecture.mmd](pms-api-architecture.mmd).
- [PMS API components (SVG)](pms-api-components.svg) shows component relationships and cross-cutting configuration. Its Mermaid source is [pms-api-components.mmd](pms-api-components.mmd).
- [Database schema (SVG)](database-schema.svg) is a quick relationship overview. The complete Mermaid ER diagram is in [database-schema.mmd](database-schema.mmd).
- [Infrastructure (SVG)](infrastructure.svg) shows the local setup and the Azure resources defined in Terraform. Its editable Mermaid source is [infrastructure.mmd](infrastructure.mmd).

Open the SVG files directly in a browser or image viewer. The schema source of truth is the Flyway migrations in `services/pms-api/src/main/resources/db/migration`. The Azure view reflects Terraform resources only: Terraform does not currently deploy a `pms-api` container or wire its secrets and telemetry.

See the [PMS API test guide](../testing.md) for running tests, viewing reports, current coverage, and recommended follow-up coverage.
