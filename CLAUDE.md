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

Frontend and backend are independent builds — no Maven integration. During development, use Angular MCP `devserver.start` with a proxy to the backend API.

## Domain

Multi-tenant B2B SaaS for dance school management. Each **School** is a tenant. See `docs/GLOSSARY.md` for ubiquitous language (DDD-style).

### Product Roadmap

**Phase 1 — DanceStudio Manager Platform (current)**
- Admin portal (Angular frontend) for school Owners and Teachers
- Office-oriented: manage sign-ups, payments, organize helpers when students cancel (partner matching for couple dance)
- Multi-tenant B2B — each school is a tenant

**Phase 2 — Student App (future, Android/iOS)**
- Students browse dance schools, view classes, enroll
- Social login (Apple, Google, possibly Instagram) — frictionless onboarding is a priority
- Dance events feed — community-oriented: school owners post events, plus external sources (scraped data, festival organizations)
- Community building is a core goal

**Business model:** Solve admin pain for teachers → get students onto the platform → build community via events → monetize via ads, school subscriptions, and other revenue streams TBD

### Authentication Strategy

- **Local dev:** Spring Security form login with in-memory users (`owner@test.com` / `password` as OWNER, `user@test.com` / `password` as USER). Dev users and a school are seeded on startup by `DevDataSeeder` — no onboarding needed.
- **Production:** Firebase JWT auth (Google sign-in via Firebase SDK). Stateless, token-based. Activated by setting `app.security.dev-auth=false` and configuring the `prod` Spring profile.
- **Future:** OAuth/social login for all users (owners, teachers, students) via a managed provider (Auth0, Clerk, or similar). Decision pending on provider choice.

## CI/CD

- **Branch protection** is enabled on `main` — all changes go through PRs
- CI workflows in `.github/workflows/`: `ci.yml` (tests), `codeql.yml` (code scanning)
- **Frontend** deployed on Render (static site): https://danceschool-2g1m.onrender.com/
- **Backend** deployed on Render (Docker): https://danceschool-api.onrender.com/ (API base: `/api`)

## Git & GitHub

- **GitHub repo:** `lruppe/danceschool`
- **Git operations** (commit, push, pull, branch): use `git` CLI
- **GitHub operations** (PRs, checks, issues): use `gh` CLI
- **All changes go through PRs** — never commit directly to `main`
- **Always work in a worktree** — use Claude Code's built-in worktree tools (`EnterWorktree`) or `claude --worktree <name>`. This creates worktrees at `.claude/worktrees/<name>/` inside the project. Do NOT use raw `git worktree add` to create worktrees outside the project directory — external worktrees break MCP tool context and file watching.
- **Fresh worktrees need setup** — always run `npm install` in `frontend/` before building or starting the dev server in a new worktree
- **Workflow:** pull main → create worktree → create branch → commit → rebase on main → push → create PR → `gh pr checks <number> --watch` → squash merge → pull main
- **Auto-merge:** merge immediately after CI passes, no manual review needed

## Visual Testing Workflow

When verifying frontend changes visually (layout, styling, component rendering), follow this workflow:

### 1. Start both servers

- **Backend:** `cd backend && ./mvnw spring-boot:run` (runs on `http://localhost:8080`)
- **Frontend:** Angular MCP `devserver.start` (workspace: `frontend/`) — uses `proxy.conf.json` to forward `/api` requests to the backend
- Wait for both to be ready before proceeding

### 2. Log in

- **Dev credentials:** `owner@test.com` / `password` (OWNER role) or `user@test.com` / `password` (USER role)
- The login page shows a simple email/password form with quick-login buttons for each role
- Use Playwright MCP to fill the form or click a quick-login button

### 3. Handle fresh database state

The backend uses H2 in-memory, so every restart is a clean slate. However, `DevDataSeeder` automatically seeds dev users + a school on startup, so login lands directly in the app shell — no onboarding step needed.

### 4. Navigate and screenshot

- Use Playwright `browser_navigate` to the relevant page
- Use `browser_take_screenshot` to visually verify layout and styling
- **Always take a final screenshot before committing** style or layout changes

## Working Rules

- **Always update the relevant CLAUDE.md** with learnings discovered during work (e.g., environment details, confirmed conventions, corrected assumptions). CLAUDE.md files should stay accurate and evolve as the project does.
