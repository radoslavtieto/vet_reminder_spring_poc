# GitHub Copilot instructions

Read and follow the repository-wide rules in [`AGENTS.md`](../AGENTS.md). These instructions apply to Copilot Chat, code completion, and Copilot coding agents working in this repository.

## Working in this repository

- Treat the three services as separate deployable applications with distinct data ownership. Preserve the boundaries described in `README.md` and `AGENTS.md`.
- Check the existing implementation before assuming a README roadmap item is implemented. The README is an intended design in places, not evidence of completed code.
- Prefer a narrow, test-backed change over broad scaffolding. Do not create speculative APIs, schemas, event contracts, or Azure infrastructure.
- Ask before resolving a significant open architecture or business decision from README section 14.
- Keep changes platform-neutral for Windows 11 and Ubuntu. Do not require shell-specific scripts for the primary build and test path.
- Never include real personal/clinical data or secrets. Never run deployment, infrastructure provisioning, or other externally mutating Azure commands without explicit user approval.

## Pull requests and handoffs

- Describe behavior and architecture changes clearly, with the tests run and any that could not run.
- Flag breaking API or event-contract changes and identify the required compatibility/versioning decision.
- Do not claim that deployments, cloud resources, tests, or roadmap work exist unless they were actually completed.
