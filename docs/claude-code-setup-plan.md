# Plan: Mirror the GitHub agentic setup for Claude Code

## Context

The repo already has a full GitHub Copilot setup under `.github/` (3 agents, 4 prompts, 3 skills, 3 instruction files) and a root `CLAUDE.md`. Claude Code currently reads only `CLAUDE.md`, which does not import the rules in `AGENTS.md` and does not expose the agents, skills or prompts. There is no `.claude/` directory. The goal is to give Claude Code the same behaviour and workflows, while keeping `AGENTS.md` as the single source of engineering rules so the two toolchains do not drift.

## Recommended approach

Keep `AGENTS.md` as the authority. Port each Copilot artifact to its Claude Code equivalent, and leave the Copilot originals in place so Copilot keeps working.

1. **`CLAUDE.md`**: add `@AGENTS.md` at the top so the rules load as an import. Fix the Maven wrapper contradiction: `mvnw` and `mvnw.cmd` exist at the root, so `AGENTS.md` (which says otherwise) should be corrected, not `CLAUDE.md`. Keep `CLAUDE.md` focused on commands and architecture.

2. **Agents → `.claude/agents/*.md`**. Port `planner`, `reviewer`, `implementer`. Keep `name` and `description`. Add a `tools:` line to enforce the read-only rule instead of relying on prose:
   - `vetsync-planner`, `vetsync-reviewer`: `Read, Grep, Glob` (no Edit/Write/Bash).
   - `vetsync-implementer`: full toolset.
   Body text stays the same, with the `.github/copilot-instructions.md` reference replaced by `CLAUDE.md`.

3. **Skills → `.claude/skills/*/SKILL.md`**. Copy `service-boundaries`, `tdd-java`, `github-actions` unchanged. Frontmatter (`name`, `description`) already matches the Claude format.

4. **Prompts → `.claude/commands/*.md`**. Copy `review-change`, `start-feature`, `add-sync-stream`, `change-event-contract`. Replace `${input:request:...}` with `$ARGUMENTS` and drop `mode: agent`.

5. **Instructions (`applyTo` globs)**. No direct equivalent. Two options:
   - (a) Fold the rules into `AGENTS.md` and the relevant `CLAUDE.md` sections, since they are short.
   - (b) Use `.claude/rules/*.md` with path frontmatter, if the installed Claude Code version supports it. Verify before relying on it.
   Recommend (a) unless the files grow.

6. **`.claude/settings.json`** (checked in, no secrets): a `permissions` block that allows the routine read-only and Maven commands used by the docs (`./mvnw -B ...`, `docker compose ...`), and denies force-push and `az deployment`/`azd up` so the "no Azure deployment without approval" rule is enforced, not just written. Optional `settings.local.json` for personal overrides, gitignored.

7. **Not ported**: `.github/copilot-instructions.md` (Copilot-only, its content is covered by `CLAUDE.md`), `.github/modernize/` (tool-internal, gitignored), and the workflows (no agent involvement).

## Critical files

- Edit: `CLAUDE.md`, `AGENTS.md` (Maven wrapper line)
- Create: `.claude/agents/vetsync-{planner,reviewer,implementer}.md`, `.claude/skills/{service-boundaries,tdd-java,github-actions}/SKILL.md`, `.claude/commands/{review-change,start-feature,add-sync-stream,change-event-contract}.md`, `.claude/settings.json`
- Read-only source: `.github/agents/*`, `.github/skills/*`, `.github/prompts/*`, `.github/instructions/*`

## Verification

- `git status` shows only the intended new and edited files under `.claude/`, `CLAUDE.md`, `AGENTS.md`. Nothing under `.github/` changes.
- Start Claude Code in the repo; `/agents` lists the three VetSync agents, `/skills` lists the three skills, and `/` lists the four new commands.
- Confirm the reviewer agent cannot edit: ask it to change a file and check it has no Edit/Write tool.
- Run `./mvnw -B -pl services/sync-service -am verify` once to confirm the documented command still works after the `AGENTS.md` correction.
- Check that `settings.json` denies `azd up` and allows `./mvnw` by running a dry permission check in the session.

## Open decisions

1. Port instructions by folding into `CLAUDE.md`/`AGENTS.md` (recommended), or use `.claude/rules/` path rules?
2. Commit `.claude/settings.json` to the repo (shared rules), or keep permissions personal?
3. Should the Copilot files stay as they are, or be removed once Claude Code is the primary tool?
