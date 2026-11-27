# Plan: one source for Claude Code and GitHub Copilot agent content

Status: proposed, not implemented. Open questions are listed at the end.

## Context

`.claude/` and `.github/` hold largely the same agentic content, written twice:

- **Agents:** 3 each (planner, reviewer, implementer). Bodies are near-identical. Only the name of the rules file differs (`CLAUDE.md` vs `.github/copilot-instructions.md`).
- **Commands / prompts:** 4 each (review-change, start-feature, add-sync-stream, change-event-contract). Bodies are identical except argument syntax (`$ARGUMENTS` vs `${input:...}`).
- **Skills:** 3 each (service-boundaries, tdd-java, github-actions). Bodies are identical.

Only the wrappers differ: frontmatter fields and file names. Nothing checks that the two copies stay in sync, so they will drift. `AGENTS.md` is already the stated rule authority (`CLAUDE.md` imports it and says the `.github` files mirror it). `docs/claude-code-setup-plan.md` describes `.claude/` as a port of `.github/`. The duplication is a maintenance risk, not a deliberate design.

**Goal:** author each agent, prompt, and skill once, generate the tool-specific files from that source, and fail CI when the generated files drift.

## Recommended approach

1. **Single canonical source.** Author each item once in `agent-src/`, using neutral frontmatter (`name`, `description`) plus an optional `claude:` or `copilot:` block for tool-specific fields such as `tools` or `applyTo`.
2. **Generator script.** One script writes:
   - `.claude/agents/*.md`, `.claude/commands/*.md`, `.claude/skills/*/SKILL.md`
   - `.github/agents/*.agent.md`, `.github/prompts/*.prompt.md`, `.github/skills/*/SKILL.md`

   It translates argument syntax per target (`$ARGUMENTS` ↔ `${input:...}`) and substitutes the rules-file reference per target.
3. **Keep the rules in `AGENTS.md`.** Both generated trees only point at it, which matches the existing authority statement.
4. **Drift check.** Run the generator in CI and fail when `git diff` is non-empty afterwards.
5. **Leave tool-only config alone.** These stay hand-written: `.claude/settings.json`, `.github/copilot-instructions.md`, `.github/instructions/java.instructions.md`, `.github/instructions/documentation.instructions.md`, `.github/workflows/`, and the issue and PR templates. `github-actions.instructions.md` may later fold into the github-actions source.

### Alternatives considered (not recommended)

- **Symlinks or junctions from `.claude/` into `.github/`.** Fragile on Windows (needs developer mode or admin, and git `core.symlinks`). The frontmatter still differs, so the format problem remains.
- **Point Claude at `.github/` directly.** Claude Code reads agents, commands, and skills only from `.claude/` paths, so this needs a generator anyway.
- **Manual mirroring plus a CI diff check.** Cheapest, but every change needs two edits, and the check only catches drift after the fact.

## Critical files

- Generated targets: `.claude/agents/`, `.claude/commands/`, `.claude/skills/`, `.github/agents/`, `.github/prompts/`, `.github/skills/`
- Source (new): `agent-src/`
- Rule authority, unchanged: `AGENTS.md`, `CLAUDE.md`
- Update: `docs/claude-code-setup-plan.md` (replace the "port by hand" note with a pointer to this plan or to the generator)
- Add a drift-check job under `.github/workflows/`, scoped to `agent-src/` and the generated paths

## Open questions

1. **Generator language.** PowerShell matches the Windows setup but needs a POSIX twin for Linux CI. Java runs on any OS with no new runtime. Node is portable but adds a runtime to a Maven project.
2. **The Claude-only `tools:` field.** The planner and reviewer agents restrict their tools. Keep it in a `claude:` block (recommended), or drop it? The Copilot `.agent.md` files in this repo have no tools field.

## Caveats

- The frontmatter facts above come only from the files in this repo. Field names must be checked against the current Claude Code and GitHub Copilot documentation before the generator is written.
- No workflow reads these files today, so no existing automation will break.

## Verification (once implemented)

1. Run the generator on the current content. The output should match the existing `.claude/` and `.github/` files except for intended translations, and the diff should be small and explainable.
2. Edit one source file, regenerate, and confirm both trees update.
3. Hand-edit a generated file, run the drift check, and confirm it fails.
4. Open a Claude Code session and a Copilot Chat session and confirm each lists the agents, commands or prompts, and skills.
