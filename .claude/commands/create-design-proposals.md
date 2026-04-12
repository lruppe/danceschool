Produce N visual design proposals for a single frontend page, screenshot each one, and save them where the user can review side-by-side.

## Input

The user provides the page to redesign and (optionally) a reference image:
- `/create-design-proposals courses` → 3 proposals for the courses list page
- `/create-design-proposals courses 4` → 4 proposals
- `/create-design-proposals course-detail --ref "C:\Users\leon_\Pictures\...\ref.png"` → proposals informed by a reference

Defaults: **3 proposals**, no reference.

## Phase 1 — Understand the page

1. Locate the component (template + scss + ts). Read them.
2. If a reference image was provided, view it and list what makes it distinct (layout, typography, color, density, chip/pill style, card vs flat, etc.).
3. Ask the user for a 1–2 sentence design goal if it isn't obvious ("calmer", "more density", "match Figma", ...). Skip if the goal is clear from the reference or the invoking message.

## Phase 2 — Propose N directions

Generate N meaningfully different proposals, not minor variations. Pick axes that matter for this page. Examples:
- **Surface**: card-wrapped vs flat on page bg vs elevated
- **Chrome density**: outlined form fields vs filled vs ghost
- **Tab placement**: in the card vs above it vs hidden behind a toggle
- **Status indicator**: emoji vs colored dot vs pill fill
- **Typography scale**: Material per-region default vs unified 2-scale (chrome/content)

Write a one-paragraph brief for each proposal before touching code so the user can see the plan.

## Phase 3 — Set up infrastructure (once)

Follow the setup + visual-testing steps from `.claude/commands/start-issue.md` (Phase 1 worktree + deps, Phase 4 servers + Playwright login). The same rules apply: run in a worktree, install frontend deps, start backend on a random port, start the Angular dev server via Angular MCP, log in via Playwright at 1440×900, and navigate to the target page.

Before starting the per-proposal loop:
- Create a **fresh output directory** for this run so previous variants aren't overwritten: `/mnt/c/Users/<user>/Pictures/Dance School/<page>-<YYYYMMDD-HHMM>/` (e.g. `courses-20260412-1430/`). Use `mkdir -p`.
- Take a **v0-baseline.png** screenshot of the current page into that directory.

## Phase 4 — For each proposal, repeat

1. Apply the minimal edits for this proposal (usually just the component's html + scss, occasionally `_table.scss` / `_tokens.scss`). Commit nothing.
2. Wait for devserver rebuild (Angular MCP `devserver.wait_for_build`).
3. Refresh the Playwright page if needed and take a viewport screenshot named `v<N>-<short-name>.png`.
4. Copy the screenshot from the Playwright output directory into the Pictures output dir.
5. **Revert the edits** with `git checkout -- <files>` so the next proposal starts from a clean `main`.

Never commit the proposals. This command produces screenshots, not PRs.

## Phase 5 — Report

Present the user with:
- The absolute path to the output directory
- A table listing each file + the one-line description of what that proposal does
- A short ask: "Which direction do you want me to build for real? Or a mix — e.g. 'V2 with V1's subtitle'."

## Rules

- **Never skip the revert step** between proposals. Mixing leaves the working tree dirty and compounds changes.
- **Always screenshot at 1440×900** (per project feedback memory).
- **Don't invent features** — keep each proposal a visual-only rework. If a proposal needs a new column or new data, flag it as out-of-scope and move on.
- **Respect design tokens** (see `frontend/CLAUDE.md` — no hardcoded spacing/colors/typography). If a proposal needs a new token, add it to `_tokens.scss` in the same cycle.
- **Login is session-based in dev** — click Sign in rather than navigating directly to the app route, since the session cookie must be established first.
- **Playwright output path** is bound to where the session started. If screenshots land in a worktree directory, `cp` them to the Pictures output dir before reverting.
