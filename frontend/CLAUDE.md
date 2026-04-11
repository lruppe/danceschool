# Frontend — Angular

## Build & Run Commands

- **Install:** `npm install`
- **Build:** `ng build`
- **Test:** `npx ng test --browsers chromium --no-watch` (or use Angular MCP `test`)
  - Requires `@vitest/browser-playwright` + `playwright` (already installed)
  - Browser binary: `npx playwright install chromium` (already done)
- **Lint:** `ng lint`

All commands run from the `frontend/` directory.

## Architecture

Angular 21 application using Angular Material for UI components.

**Key stack:**
- Angular 21 (standalone components, signals, new control flow)
- Angular Material 21 (custom SCSS theme)
- Firebase SDK (authentication via Google sign-in)
- TypeScript strict mode, SCSS

## Authentication

### Two modes (controlled by `environment.useDevLogin`)

**Dev mode** (`useDevLogin: true` — default in `environment.ts`):
- No Firebase SDK initialization — Firebase is not loaded at all
- `AuthService` uses form login: POSTs credentials to Spring's `/login` endpoint
- Session-based auth — browser session cookie handles authentication automatically
- `authInterceptor` adds `withCredentials: true` (no Bearer token)
- Login page shows email/password form with quick-login buttons for Owner and User roles
- Dev users: `owner@test.com` / `password` (School 1), `owner2@test.com` / `password` (School 2)

**Prod mode** (`useDevLogin: false` — set in `environment.prod.ts`):
- Firebase SDK (`firebase` npm package) handles Google sign-in and token management
- **No `@angular/fire`** — uses the Firebase JS SDK directly (dynamic imports)
- `AuthService` initializes Firebase, listens to `onAuthStateChanged`, manages auth state via signals
- All API calls include `Authorization: Bearer <firebase-id-token>` via `authInterceptor`
- Auth guard waits for Firebase to restore session from IndexedDB before making decisions (async)
- Login page shows a Google sign-in button

### Environment config
- `src/environments/environment.ts` — dev config (`useDevLogin: true`, `useEmulators: false`)
- `src/environments/environment.prod.ts` — prod config (Firebase project: `dance-school-ch`, `useDevLogin: false`)
- Firebase config (apiKey, authDomain, projectId) is compile-time, not secret

### Key files
- `shared/auth/auth.service.ts` — dual-mode auth: dev (form login) or prod (Firebase). Manages state via signals.
- `shared/auth/auth.interceptor.ts` — dev: withCredentials; prod: Bearer token. Redirects on 401.
- `shared/auth/auth.guard.ts` — async guard, waits for auth check to complete
- `auth/login/login.ts` — dev: email/password form with quick-login; prod: Google sign-in button

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

## Testing

1. **Business logic** — calculations, data transformations, derived values, formatting. Pure unit tests.
2. **Data-display correctness** — components that render dynamic data (tables, summaries, detail views). Render with mock data, assert all expected fields/columns are present in the output.
3. **Conditional rendering** — things that show/hide based on state. Render with different inputs, assert presence/absence of elements.

## Angular Rules

Before writing or modifying Angular code, always call Angular MCP `get_best_practices` (with `workspacePath`) and `list_projects`. Follow those rules — they are version-specific and authoritative.
