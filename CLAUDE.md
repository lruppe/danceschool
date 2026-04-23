# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

```
danceschool/
├── frontend/          ← Angular 21 (SCSS, standalone components) — see frontend/CLAUDE.md
├── backend/           ← Spring Boot backend (Java 21) — see backend/src/CLAUDE.md
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── .mvn/
│   └── src/
└── .mcp.json          ← MCP server config (Angular CLI + Playwright browser)
```

Frontend and backend are independent builds — no Maven integration. During development, use Angular MCP `devserver.start` with a proxy to the backend API.

## Domain

Multi-tenant B2B SaaS for dance school management. Each **School** is a tenant. See `docs/GLOSSARY.md` for ubiquitous language (DDD-style).

### Authentication Strategy

- **Local dev:** Spring Security form login with in-memory users (`owner@test.com` / `password` and `owner2@test.com` / `password`, both OWNER). Dev users and two separate schools are seeded on startup by `DevDataSeeder` — no onboarding needed.
- **Production:** Firebase JWT auth (Google sign-in via Firebase SDK). Stateless, token-based. Activated by setting `app.security.dev-auth=false` and configuring the `prod` Spring profile.
- **Future:** OAuth/social login for all users (owners, teachers, students) via a managed provider (Auth0, Clerk, or similar). Decision pending on provider choice.

## CI/CD

- **Branch protection** is enabled on `main` — all changes go through PRs
- CI workflows in `.github/workflows/`: `ci.yml` (tests), `codeql.yml` (code scanning)
- **Frontend** deployed on Render (static site): https://app.aquisebaila.ch/ (Render URL: `danceschool-2g1m.onrender.com`)
- **Backend** deployed on Render (Docker): https://api.aquisebaila.ch/ (API base: `/api`; Render URL: `danceschool-api.onrender.com`)

## Issue Workflow

Use `/start-issue <number>` to work on a GitHub issue. This runs the full automated lifecycle:

**Setup** → load issue, create worktree + branch, install deps
**Plan** → enter plan mode for non-trivial issues (use judgment to skip for trivial fixes)
**Implement** → code, build, test
**Visual Testing Workflow** → start both servers, login via Playwright, screenshot affected pages
**Ship** → commit, rebase, push, PR, watch CI, squash merge

**Do not stop between phases** unless genuinely blocked. Auto-merge after CI passes — no manual review needed.

See `.claude/commands/start-issue.md` for the full step-by-step.

## Git & GitHub

- **GitHub repo:** `lruppe/danceschool`
- **Git operations** (commit, push, pull, branch): use `git` CLI
- **GitHub operations** (PRs, checks, issues): use `gh` CLI
- **All changes go through PRs** — never commit directly to `main`
- **Always work in a worktree** — use Claude Code's built-in worktree tools (`EnterWorktree`) or `claude --worktree <name>`. This creates worktrees at `.claude/worktrees/<name>/` inside the project. Do NOT use raw `git worktree add` to create worktrees outside the project directory — external worktrees break MCP tool context and file watching.
- **Fresh worktrees need setup** — always run `npm install` in `frontend/` before building or starting the dev server in a new worktree
- **Auto-merge:** merge immediately after CI passes, no manual review needed

## Visual Testing Workflow

Required for all frontend changes. Skip only for purely backend issues with no UI impact. The full procedure (start servers, login, screenshot, stop) is in `.claude/commands/start-issue.md` Phase 4.

**Key details for reference:**
- **Backend:** `cd backend && ./mvnw spring-boot:run` (runs on `http://localhost:8080`)
- **Frontend:** Angular MCP `devserver.start` (workspace: `frontend/`) — uses `proxy.conf.json` to forward `/api` requests to the backend
- **Dev credentials:** `owner@test.com` / `password` (School 1) or `owner2@test.com` / `password` (School 2)
- **Database:** H2 in-memory — every restart is a clean slate, but `DevDataSeeder` seeds two owners with separate schools automatically

## Working Rules

- **Always update the relevant CLAUDE.md** with learnings discovered during work (e.g., environment details, confirmed conventions, corrected assumptions). CLAUDE.md files should stay accurate and evolve as the project does.
