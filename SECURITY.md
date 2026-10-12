# Security policy

## Reporting a vulnerability

Do not report security vulnerabilities in public issues. Use GitHub's private vulnerability reporting feature for this repository, if enabled, or contact the repository maintainers privately.

Include the affected component, impact, reproduction details, and any suggested mitigation. Do not include real customer or clinical data in a report.

## Security expectations for contributions

- Never commit secrets, access tokens, private keys, production identifiers, or real personal/clinical data.
- Keep Azure authentication on GitHub OIDC; do not introduce long-lived cloud credentials.
- Keep workflow permissions least-privilege and deployment behind explicit environment protection.
- Validate tenant and resource ownership at application boundaries; do not rely on caller-supplied identifiers alone.
- Handle sensitive values carefully in logs, errors, fixtures, and test output.
