# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

```
danceschool/
├── frontend/          ← Angular 21 (SCSS, standalone components)
├── src/               ← Spring Boot backend (Java 21)
├── pom.xml            ← Maven (backend only)
└── .mcp.json          ← MCP server config (Angular CLI + Playwright browser)
```

Frontend and backend are independent builds — no Maven integration. During development, run `ng serve` with a proxy to the backend API.

## CI/CD

- **Branch protection** is enabled on `main` — all changes go through PRs
- CI workflows in `.github/workflows/`: `ci.yml` (tests), `codeql.yml` (code scanning)
- Deployment to Hostinger VPS planned but not yet configured

## Git & GitHub

- **GitHub repo:** `lruppe/danceschool`
- **Auth:** `gh auth setup-git` provides credentials for all git and GitHub operations
- **Git operations** (commit, push, pull, branch): use `git` CLI
- **GitHub operations** (PRs, checks, issues): use GitHub MCP tools or `gh` CLI
- **All changes go through PRs** — never commit directly to `main`
- **Always work in a worktree** — start with `claude --worktree <name>` or create one at the beginning of a task
- **Workflow:** pull main → create worktree → create branch → commit → push → create PR → `gh pr checks <number> --watch` → squash merge → pull main
- **Merge strategy:** squash merge
- **Auto-merge:** merge immediately after CI passes, no manual review needed

## Working Rules

- **Always update the relevant CLAUDE.md** with learnings discovered during work (e.g., environment details, confirmed conventions, corrected assumptions). CLAUDE.md files should stay accurate and evolve as the project does.
