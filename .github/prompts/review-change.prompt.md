---
mode: agent
description: Review the current VetSync changes for actionable correctness and boundary issues.
---

Review the current diff against its base. Do not edit files. Read the relevant surrounding code and tests before reaching conclusions.

Report only actionable issues, ordered by severity. Include file and line, a concrete failure scenario, and the reason it is a defect. Check tenant isolation, service/data ownership, event compatibility and idempotency, secrets/configuration, and test coverage as relevant. Distinguish an actual bug from an unverified assumption. If there are no findings, say so and identify any significant validation gaps.
