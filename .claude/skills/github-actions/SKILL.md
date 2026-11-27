---
name: github-actions
description: Use when creating or modifying VetSync GitHub Actions workflows, permissions, or release automation.
---

# VetSync GitHub Actions changes

- Preserve one independently scoped CI/CD workflow per service.
- Ensure pull-request CI does not require secrets and deployment cannot run from an ordinary pull request.
- Scope `contents: read` and other permissions to the minimum needed. Grant `id-token: write` only to Azure deployment jobs.
- Keep cloud deployment manual and opt-in. Use the protected GitHub Environment and OIDC; never add service-principal passwords or other long-lived credentials.
- Do not add production release, infrastructure provisioning, resource deletion, or automatic deploy-on-push behavior without explicit user approval.
- Keep build paths rooted at the repository root when building a multi-module Maven project or image.
- For Maven validation, use JDK 21 and the affected service module with `-am`.
- Pin third-party actions to reviewed immutable commit SHAs when changing or adding action references; include a comment with the action version for maintainability.
- Review trigger/path filters, fork behavior, job dependencies, concurrency, environment protection, and shell-input quoting after changes.
