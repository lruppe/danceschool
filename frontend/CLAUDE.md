# Frontend — Angular

## Build & Run Commands

- **Install:** `npm install`
- **Dev server:** `ng serve` (or use Angular MCP `devserver.start`)
- **Build:** `ng build`
- **Test:** `ng test` (or use Angular MCP `test`)
- **Lint:** `ng lint`

All commands run from the `frontend/` directory.

## Architecture

Angular 21 application using Angular Material for UI components.

**Key stack:**
- Angular 21 (standalone components, signals, new control flow)
- Angular Material 21 (custom SCSS theme)
- TypeScript strict mode, SCSS

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
