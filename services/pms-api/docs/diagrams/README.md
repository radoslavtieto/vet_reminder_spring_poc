# PMS API diagrams

These diagrams describe the checked-in `pms-api` implementation and infrastructure scaffold:

- [Database schema (SVG)](database-schema.svg) is a quick relationship overview. The complete Mermaid ER diagram is in [database-schema.mmd](database-schema.mmd).
- [Infrastructure (SVG)](infrastructure.svg) shows the local setup and the Azure resources defined in Terraform. Its editable Mermaid source is [infrastructure.mmd](infrastructure.mmd).

Open the SVG files directly in a browser or image viewer. The schema source of truth is the Flyway migrations in `services/pms-api/src/main/resources/db/migration`. The Azure view reflects Terraform resources only: Terraform does not currently deploy a `pms-api` container or wire its secrets and telemetry.
