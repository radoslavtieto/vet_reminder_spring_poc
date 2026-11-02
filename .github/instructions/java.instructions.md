---
applyTo: "**/*.java"
---

# Java and Spring Boot

- Target Java 21 and use the Spring Boot and Maven conventions already established by the root and service POMs.
- Keep the domain independent of Spring, persistence annotations, HTTP types, and vendor SDKs wherever practical.
- Put orchestration in application use cases; expose interfaces (ports) for external I/O and implement them in adapters.
- Keep web and messaging adapters thin: validate/translate input, call application behavior, and map results.
- Use constructor injection and explicit, narrow types. Avoid static service locators and broad exception catches.
- Inject `Clock` for time-dependent domain behavior; avoid direct system-clock calls in domain code.
- Add or update focused unit tests for domain/application behavior and adapter/context tests for integration behavior.
- Keep PMS types out of sync and action-engine code. Shared event types belong in `libs/sync-contracts`; do not duplicate producer/consumer contracts in the service modules.
