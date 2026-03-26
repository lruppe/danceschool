# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

```
danceschool/
├── frontend/          ← Angular 21 (SCSS, standalone components)
├── backend/           ← Spring Boot backend (Java 21)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── .mvn/
│   └── src/
└── .mcp.json          ← MCP server config (Angular CLI + Playwright browser)
```

Frontend and backend are independent builds — no Maven integration. During development, run `ng serve` with a proxy to the backend API.

## Domain

Multi-tenant B2B SaaS for dance school management. Each **School** is a tenant. See `docs/GLOSSARY.md` for ubiquitous language (DDD-style).

- **Admin portal** (current Angular frontend): for school Owners and Teachers
- **Student apps** (future): students browse schools, view classes, enroll

## CI/CD

- **Branch protection** is enabled on `main` — all changes go through PRs
- CI workflows in `.github/workflows/`: `ci.yml` (tests), `codeql.yml` (code scanning)
- **Frontend** deployed on Render (static site): https://danceschool-2g1m.onrender.com/
- **Backend** deployed on Render (Docker): https://danceschool-api.onrender.com/ (API base: `/api`)

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

## Visual Testing Workflow

When verifying frontend changes visually (layout, styling, component rendering), follow this workflow:

### 1. Start both servers

- **Backend:** `cd backend && ./mvnw spring-boot:run` (runs on `http://localhost:8080`)
- **Frontend:** Angular MCP `devserver.start` (workspace: `frontend/`) — uses `proxy.conf.json` to forward `/api` requests to the backend
- Wait for both to be ready before proceeding

### 2. Log in

- **Credentials:** `dance_admin` / `DanceSchool2024!` (single in-memory admin user, configured in backend — see `backend/src/CLAUDE.md` for details)
- Use Playwright MCP to fill the login form and click Sign in

### 3. Handle fresh database state

The backend uses H2 in-memory, so every restart is a clean slate:
- If the admin has **no school**, login redirects to `/onboarding` — create a school first to reach the main app shell
- If you only need to verify the shell layout (sidebar, toolbar), the onboarding page already shows it

### 4. Navigate and screenshot

- Use Playwright `browser_navigate` to the relevant page
- Use `browser_take_screenshot` to visually verify layout and styling
- **Always take a final screenshot before committing** style or layout changes

## Working Rules

- **Always update the relevant CLAUDE.md** with learnings discovered during work (e.g., environment details, confirmed conventions, corrected assumptions). CLAUDE.md files should stay accurate and evolve as the project does.
