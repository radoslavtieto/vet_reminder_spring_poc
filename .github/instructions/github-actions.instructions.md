---
applyTo: ".github/workflows/**"
---

# GitHub Actions

- Preserve the separate workflow and deployment boundary for each service.
- Keep workflow permissions least-privilege and job-scoped where possible; grant `id-token: write` only to jobs that authenticate to Azure with OIDC.
- Do not introduce long-lived cloud credentials, secrets in workflow YAML, or unreviewed external side effects.
- Keep deployments manual and opt-in unless the user explicitly requests a different release policy.
- Do not add production deployment, infrastructure provisioning, or resource deletion without explicit approval and corresponding safeguards.
- Keep Maven validation scoped to the service when practical, including required reactor modules with `-am`.
- Pin third-party actions to reviewed immutable commit SHAs when changing or adding action references; document the upstream version in a YAML comment.
- When changing a workflow, check its triggers, permissions, environment protection, concurrency, and whether forked pull requests could reach secrets.
