# Frontend — Angular

## Build & Run Commands

- **Install:** `npm install`
- **Build:** `ng build`
- **Test:** `ng test --browsers chromium --no-watch` (or use Angular MCP `test`)
  - Requires `@vitest/browser-playwright` + `playwright` (already installed)
  - Browser binary: `npx playwright install chromium` (already done)
- **Lint:** `ng lint`

All commands run from the `frontend/` directory. For the dev server, see the workflow below.

## Frontend Development Workflow

Use MCP tools for the full develop → verify loop:

1. **Install dependencies:** Run `npm install` if in a fresh worktree or after pulling new changes
2. **Verify build first:** Run `npx ng build` to confirm compilation before starting the dev server. This catches errors faster than the MCP devserver.
3. **Start dev server:** Angular MCP `devserver.start` (workspace: `frontend/`)
4. **Wait for build:** Angular MCP `devserver.wait_for_build` — also call this after every code change to confirm it compiled
   - **Fallback:** If `wait_for_build` returns the same error after you've fixed the code, don't cycle stop/restart — run `npx ng build` directly to get the real build status. The MCP devserver can cache stale errors.
5. **Visual verify:** Playwright MCP `browser_navigate` to the dev server URL, then `browser_snapshot` (DOM/accessibility tree) or `browser_take_screenshot` (visual)
6. **Iterate:** edit code → `devserver.wait_for_build` → snapshot/screenshot → repeat
7. **Visual verify before commit:** MUST take a final screenshot and confirm no visual regressions before committing. Never commit style or layout changes without visually verifying the result.
8. **Stop server:** Angular MCP `devserver.stop` when done

### Playwright MCP
- Use `browser_snapshot` over `browser_take_screenshot` when you need to interact with elements (click, fill, etc.)
- Use `browser_take_screenshot` to visually verify layout and styling

## Architecture

Angular 21 application using Angular Material for UI components.

**Key stack:**
- Angular 21 (standalone components, signals, new control flow)
- Angular Material 21 (custom SCSS theme)
- Firebase SDK (authentication via Google sign-in)
- TypeScript strict mode, SCSS

## Authentication — Firebase

### Architecture
- **Firebase SDK** (`firebase` npm package) handles Google sign-in and token management
- **No `@angular/fire`** — uses the Firebase JS SDK directly for simplicity and Angular 21 compatibility
- `AuthService` initializes Firebase, listens to `onAuthStateChanged`, and manages auth state via signals
- All API calls include `Authorization: Bearer <firebase-id-token>` via `authInterceptor`
- Auth guard waits for Firebase to restore session from IndexedDB before making decisions (async)
- Login page shows a Google sign-in button (no username/password)

### Firebase Emulator (local development)
- `firebase.json` at repo root configures the Auth emulator on port 9099
- Dev environment (`environment.ts`) sets `useEmulators: true`
- Start emulator: `cd frontend && npm run emulators`
- Pre-seeded test user: `test@danceschool.com` / `test123456` (imported from `emulator-data/` at repo root)

### Environment config
- `src/environments/environment.ts` — dev config with emulator flag
- `src/environments/environment.prod.ts` — prod config (Firebase project: `dance-school-ch`)
- Firebase config (apiKey, authDomain, projectId) is compile-time, not secret

### Key files
- `shared/auth/auth.service.ts` — Firebase init, `onAuthStateChanged`, `signInWithPopup`, token signals
- `shared/auth/auth.interceptor.ts` — attaches Bearer token, redirects on 401
- `shared/auth/auth.guard.ts` — async guard, waits for Firebase session restore
- `auth/login/login.ts` — Google sign-in button, redirect on success

## Design Tokens & Styling — MANDATORY

> **These rules are non-negotiable.** Every component, every style change, every PR must follow them. No exceptions. Do not introduce hardcoded spacing, color, or typography values anywhere in the frontend.

Custom design tokens (`--ds-*`) extend Material's system tokens (`--mat-sys-*`). Always read the token files before writing styles — they are the source of truth.

**Token files (read these before implementing styles):**
- `src/styles/_tokens.scss` — spacing scale, border radius, semantic colors, semantic layout, motion
- `src/styles/_mixins.scss` — responsive breakpoint mixins
- `src/styles/_index.scss` — barrel file (import via `@use 'styles' as ds;`, enabled by `includePaths` in angular.json)

**Strict rules — always enforced:**
1. **Spacing/layout:** MUST use `--ds-spacing-*` or semantic layout tokens. Hardcoded `px`, `rem`, or `em` values for margins, padding, and gaps are forbidden. If the scale doesn't have the value you need, add a new token to `_tokens.scss` — do not inline it.
2. **Colors:** MUST use `--mat-sys-*` or `--ds-color-*` tokens. No hex codes, no `rgb()`, no hardcoded color values.
3. **Typography:** MUST use `--mat-sys-*` type scale tokens (`display-*`, `headline-*`, `title-*`, `body-*`, `label-*`). No hardcoded `font-size` or `line-height`.
4. **Breakpoints:** MUST use mixins `@include ds.bp-up(md)`, `ds.bp-down(sm)`, `ds.bp-between(sm, lg)`. No hardcoded `@media` queries.
5. **Transitions:** MUST use `--ds-duration-*` and `--ds-easing-*` tokens. No hardcoded durations or easing functions.

## Angular Rules

Before writing or modifying Angular code, always call Angular MCP `get_best_practices` (with `workspacePath`) and `list_projects`. Follow those rules — they are version-specific and authoritative.
