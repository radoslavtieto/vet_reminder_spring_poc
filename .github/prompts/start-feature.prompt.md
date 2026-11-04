---
mode: agent
description: Implement a small VetSync feature using test-first development and existing service boundaries.
---

Implement this request: ${input:request:Describe the behavior to implement}

First inspect the current service implementation, repository instructions, and working-tree changes. Treat the README roadmap as proposed work unless code confirms it exists. Identify the owning service and preserve its data boundary.

For behavior changes, add a focused failing test before implementation. Implement only the requested scope, update directly related documentation, and run the narrowest relevant Maven Wrapper `verify` command with JDK 21 (`./mvnw` in Bash or `.\mvnw.cmd` in Windows PowerShell). Do not make Azure or other external changes. Report what changed, the exact verification outcome, and any blockers or unresolved decisions.
