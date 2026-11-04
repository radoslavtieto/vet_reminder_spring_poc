---
name: tdd-java
description: Use when changing Java or Spring behavior in VetSync; guides test-first implementation and Maven verification.
---

# Test-first Java changes

1. Read `AGENTS.md`, applicable `.github/instructions/`, the owning service, and the relevant test conventions.
2. Identify observable behavior and write a focused regression or behavior test that fails for the intended reason.
3. Run that test to establish the red state.
4. Implement the smallest change in the correct domain, application, or adapter layer.
5. Run the focused test, then the owning module's Maven verification:
   - `./mvnw -B -pl services/pms-api -am verify`
   - `./mvnw -B -pl services/sync-service -am verify`
   - `./mvnw -B -pl services/action-engine -am verify`

   In Windows PowerShell, use `.\mvnw.cmd` in place of `./mvnw`.
6. Review the diff for generated artifacts, unrelated edits, data-boundary violations, secrets, and missing docs.

Use JDK 21. Testcontainers tests need Docker. Report blocked validation honestly. Do not turn a Docker-dependent test into a silent skip or claim it passed when it did not run.
