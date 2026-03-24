# Frontend — Angular

## Build & Run Commands

- **Install:** `npm install`
- **Dev server:** `ng serve` (or use Angular MCP `devserver.start`)
- **Build:** `ng build`
- **Test:** `ng test --browsers chromium --no-watch` (or use Angular MCP `test`)
  - Requires `@vitest/browser-playwright` + `playwright` (already installed)
  - Browser binary: `npx playwright install chromium` (already done)
- **Lint:** `ng lint`

All commands run from the `frontend/` directory.

## Frontend Development Workflow

Use MCP tools for the full develop → verify loop:

1. **Start dev server:** Angular MCP `devserver.start` (workspace: `frontend/`)
2. **Wait for build:** Angular MCP `devserver.wait_for_build` — also call this after every code change to confirm it compiled
3. **Visual verify:** Playwright MCP `browser_navigate` to the dev server URL, then `browser_snapshot` (DOM/accessibility tree) or `browser_take_screenshot` (visual)
4. **Iterate:** edit code → `devserver.wait_for_build` → snapshot/screenshot → repeat
5. **Visual verify before commit:** MUST take a final screenshot and confirm no visual regressions before committing. Never commit style or layout changes without visually verifying the result.
6. **Stop server:** Angular MCP `devserver.stop` when done

### Playwright MCP

- Requires Google Chrome installed in WSL (`/opt/google/chrome/chrome`)
- If missing: `wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O /tmp/chrome.deb && sudo dpkg -i /tmp/chrome.deb && sudo apt --fix-broken install -y`
- Use `browser_snapshot` over `browser_take_screenshot` when you need to interact with elements (click, fill, etc.)
- Use `browser_take_screenshot` to visually verify layout and styling

## Architecture

Angular 21 application using Angular Material for UI components.

**Key stack:**
- Angular 21 (standalone components, signals, new control flow)
- Angular Material 21 (custom SCSS theme)
- TypeScript strict mode, SCSS

## Design Tokens & Styling — MANDATORY

> **These rules are non-negotiable.** Every component, every style change, every PR must follow them. No exceptions. Do not introduce hardcoded spacing, color, or typography values anywhere in the frontend.

Custom design tokens (`--ds-*`) extend Material's system tokens (`--mat-sys-*`). Defined in `src/styles/_tokens.scss`, injected globally via `src/styles.scss`.

**Strict rules — always enforced:**
1. **Spacing/layout:** MUST use `--ds-*` tokens. Hardcoded `px`, `rem`, or `em` values for margins, padding, and gaps are forbidden. If the scale doesn't have the value you need, add a new token to `_tokens.scss` — do not inline it.
2. **Colors:** MUST use `--mat-sys-*` tokens. No hex codes, no `rgb()`, no hardcoded color values.
3. **Typography:** MUST use `--mat-sys-*` type scale tokens (`display-*`, `headline-*`, `title-*`, `body-*`, `label-*`). No hardcoded `font-size` or `line-height`.
4. **Shared SCSS:** Import via `@use 'styles' as ds;` (enabled by `includePaths` in angular.json).
5. **Breakpoints:** MUST use mixins `@include ds.bp-up(md)`, `ds.bp-down(sm)`, `ds.bp-between(sm, lg)`. No hardcoded `@media` queries.
6. **Transitions:** MUST use `--ds-duration-*` and `--ds-easing-*` tokens. No hardcoded durations or easing functions.

**Allowed exceptions (only these):**
- `0` (zero) — always fine
- `100%`, `100vh`, `100vw` — full-size values
- `1px` for borders (there is no token for hairline borders)
- Intrinsic sizing (`auto`, `min-content`, `max-content`, `fit-content`)
- Grid `minmax()` column widths (e.g., `minmax(280px, 1fr)`) — these are layout breakpoints, not spacing

**Token files:**
- `src/styles/_tokens.scss` — spacing scale, semantic layout, motion
- `src/styles/_mixins.scss` — responsive breakpoint mixins
- `src/styles/_index.scss` — barrel file

**Spacing scale (4px base):** `--ds-spacing-{1,2,3,4,5,6,8,10,12,16,20,24}` → 4px–96px

**Semantic tokens:** `--ds-page-inline-padding`, `--ds-page-block-padding`, `--ds-card-padding`, `--ds-section-gap`, `--ds-content-max-width`

**Motion:** `--ds-duration-{fast,normal,slow}`, `--ds-easing-{standard,decelerate}`

## Angular Rules

- Standalone components only (no NgModules)
- Signals for state management, `computed()` for derived state
- `input()`/`output()` functions instead of decorators
- `inject()` function instead of constructor injection
- `ChangeDetectionStrategy.OnPush` on all components
- Native control flow (`@if`, `@for`, `@switch`)
- Reactive forms over template-driven
- Lazy loading for feature routes
- `providedIn: 'root'` for singleton services
- Do not use `ngClass`/`ngStyle` — use `class`/`style` bindings
- Do not use `@HostBinding`/`@HostListener` — use `host` object in decorator
- Use `NgOptimizedImage` for all static images
- Always use Angular MCP `get_best_practices` and `list_projects` before writing Angular code
