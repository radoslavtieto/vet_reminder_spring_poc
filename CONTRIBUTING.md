# Contributing

## Development setup

- JDK 21
- Maven 3.9.x
- Docker for Testcontainers-based integration tests

The Maven Wrapper and local Docker Compose stack have not yet been added. From the repository root, run:

```sh
mvn -B clean verify
```

To build and test just one service and its reactor prerequisites:

```sh
mvn -B -pl services/pms-api -am verify
mvn -B -pl services/sync-service -am verify
mvn -B -pl services/action-engine -am verify
```

## Change expectations

- Follow `AGENTS.md` and `.github/copilot-instructions.md`.
- Use test-first development for behavior changes and keep each change focused.
- Preserve service data ownership and the read-only boundary between sync-service and PMS data.
- Use synthetic test data only; do not commit credentials, personal data, or production data.
- Update relevant documentation when behavior, APIs, event contracts, or developer commands change.
- Run the narrowest relevant verification and state clearly what ran and what was blocked.

## Pull requests

Use the pull request template. Describe behavior and compatibility impact, list verification performed, and call out any unresolved architecture decisions. Do not include unrelated IDE settings or generated build output.
