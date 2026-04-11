Start working on a GitHub issue end-to-end: from loading the issue through implementation, visual verification, and merging.

## Input

The user provides a GitHub issue number as an argument (e.g., `/start-issue 100`).

## Phase 1 — Setup

1. **Load the issue:** `gh issue view <number>` (include comments for full context)
2. **Create worktree:** Use `EnterWorktree` with a descriptive name derived from the issue (e.g., `100-multi-tenancy-isolation`)
3. **Install frontend deps:** Run `npm install` in `frontend/` (fresh worktrees need this)
4. **Create branch:** Create a feature branch from main

## Phase 2 — Planning

Assess whether the issue is trivial or non-trivial:
- **Trivial** (typo, single-file change, config tweak): skip planning, go straight to implementation
- **Non-trivial** (multi-file, new feature, architectural change): enter plan mode, produce a step-by-step implementation plan, then exit plan mode and proceed

## Phase 3 — Implementation

Implement the changes according to the plan (or directly for trivial issues). Follow all project conventions in CLAUDE.md files. Run builds and tests as you go:
- **Frontend:** `npx ng build`, `npx ng test --browsers chromium --no-watch`
- **Backend:** `cd backend && ./mvnw test`

## Phase 4 — Visual Verification

**Always run this for frontend changes.** Skip only if the issue is purely backend with no UI impact.

1. **Start backend:** `cd backend && ./mvnw spring-boot:run` (background)
2. **Start frontend:** Angular MCP `devserver.start` (workspace: `frontend/`)
3. **Wait for both** to be ready
4. **Login:** Use Playwright MCP `browser_navigate` to the login page and click the Owner quick-login button (`owner@test.com` / `password`)
5. **Navigate and screenshot:** Visit all pages affected by the change. Take screenshots to verify visual correctness.
6. **Stop servers:** Stop the frontend devserver (Angular MCP `devserver.stop`) and kill the backend process

### Playwright rules
- Use `browser_snapshot` over `browser_take_screenshot` when you need to interact with elements (click, fill, etc.)
- Use `browser_take_screenshot` to visually verify layout and styling
- **Never use `browser_navigate` / `page.goto()` after the initial login** — direct URL navigation reloads the page and loses the Angular auth session. Always navigate via sidebar links and in-app buttons instead. The only `browser_navigate` call should be to the login page.
- **Always take a final screenshot before committing** style or layout changes

## Phase 5 — Ship

1. **Commit:** Stage changed files and commit with a clear message describing the change
2. **Rebase:** `git fetch origin main && git rebase origin/main` — resolve conflicts if any
3. **Push:** `git push` (or `git push --force-with-lease` after rebase)
4. **Create PR:** `gh pr create` with a summary and test plan
5. **Watch CI:** `gh pr checks <number> --watch`
6. **Merge:** `gh pr merge <number> --squash --delete-branch` once CI passes

## Important

- **Do not stop to ask** between phases unless genuinely blocked (e.g., ambiguous requirements, failing tests you can't diagnose, merge conflicts needing user judgment)
- **Auto-merge** is the default — no manual review needed
- Follow all conventions in root, frontend, and backend CLAUDE.md files
