# Claude and GitHub Copilot Guidance Consolidation Plan

## Goal

Reduce duplicated repository guidance for Claude Code and GitHub Copilot while preserving each tool's native configuration and discovery conventions.

## Proposed approach

1. **Make `AGENTS.md` the canonical source** for shared repository rules, including engineering practices, architecture boundaries, and build/test guidance. Move useful shared material currently found only in `CLAUDE.md` into `AGENTS.md` or a shared document linked from it. Check accuracy against the current repository and existing documentation before moving it.
2. **Keep tool-specific entry points concise.** `CLAUDE.md` should point Claude Code to shared guidance and contain only Claude-specific instructions. `.github/copilot-instructions.md` should do the same for Copilot.
3. **Retain native tool files.** Keep Claude and Copilot agents, commands/prompts, skills, settings, and metadata in their respective directories. Align repeated behavioral guidance where appropriate, but do not attempt to make platform-specific formats identical.
4. **Avoid unnecessary synchronization tooling.** Do not introduce symlinks, a generation system, or a parity-check tool for this consolidation.
5. **Review and verify.** Check all affected instructions for stale, contradictory, or duplicated material; inspect the full diff and confirm only intended documentation/configuration files changed.

## Scope and constraints

- This is a proposed documentation and agent-instruction cleanup; it does not change application behavior.
- Preserve Claude-specific settings and Copilot-specific instruction/agent discovery formats.
- Do not alter unrelated project instructions or resolve open business or architecture decisions.
- Distinguish implemented behavior from roadmap proposals, and keep paths and commands consistent with the checked-in project.
