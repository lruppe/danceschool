Start working on a GitHub issue end-to-end: from loading the issue through implementation, visual verification, and merging.

## Input

The user provides a GitHub issue number as an argument (e.g., `/start-issue 100`).

## Phase 1 — Setup

1. **Load the issue:** `gh issue view <number>` (include comments for full context)
2. **Update main:** `git checkout main && git pull --ff-only origin main`
3. **Create worktree:** Use `EnterWorktree` with a descriptive name derived from the issue (e.g., `100-multi-tenancy-isolation`)
4. **Install frontend deps:** Run `npm install` in `frontend/` (fresh worktrees need this)
5. **Create branch:** Create a feature branch from main

## Phase 2 — Planning

Assess whether the issue is trivial or non-trivial:
- **Trivial** (typo, single-file change, config tweak): skip planning, go straight to implementation
- **Non-trivial** (multi-file, new feature, architectural change): enter plan mode, produce a step-by-step implementation plan, then exit plan mode and proceed

## Phase 3 — Implementation

Implement the changes according to the plan (or directly for trivial issues). Follow all project conventions in CLAUDE.md files. Run builds and tests as you go — see each CLAUDE.md for the exact commands.

## Phase 4 — Visual Verification End to End

**Always run this for frontend changes.** Skip only if the issue is purely backend with no UI impact.

Multiple worktrees may run visual tests in parallel — using a random port avoids backend port conflicts.

1. **Start backend on a random port:**
   ```bash
   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=0
   ```
   Run in background. Read the output for `Tomcat started on port <PORT>` to get the actual port.
2. **Update proxy config:** Edit `frontend/proxy.conf.json` — set the target to `http://localhost:<PORT>`. This is safe per-worktree. Do not commit this change.
3. **Start frontend:** Angular MCP `devserver.start` (workspace: `frontend/`), then `devserver.wait_for_build`
   - **After code changes:** call `devserver.wait_for_build` again to confirm it compiled. If it returns the same stale error after a fix, run `npx ng build` directly — the MCP devserver can cache stale errors.
4. **Login:** Use Playwright MCP `browser_navigate` to the login page and click the Owner quick-login button (`owner@test.com` / `password`)
5. **Navigate and screenshot:** Visit all pages affected by the change. Take screenshots to verify visual correctness.
6. **Stop servers:**
   - Frontend: Angular MCP `devserver.stop`
   - Backend: `kill $(lsof -t -i:<PORT>)` (use the port from step 1)
   - Revert proxy config: `git checkout frontend/proxy.conf.json`

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
