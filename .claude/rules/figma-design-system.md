# Figma-to-Code Design System Rules

> This file covers translating Figma designs **to Angular code** — component mappings, CSS tokens, and layout constants.
> For working **inside Figma** (component structure, instancing, editing), see `figma-design-workflow.md`.

These rules define how to translate Figma designs from the Dance School Admin design file into Angular code. Follow them for every Figma implementation task.

## Figma Design File

- **Design file:** `https://www.figma.com/design/uQoQs1ZZKO8fGIwFsaLStB/Dance-School-Admin`
- **Make file (reference):** `https://www.figma.com/make/1UzKcbslBrvkxIZFiygawy/Dance-School-Admin`

## Required Implementation Flow

1. **Get design context:** Call `get_design_context` with the target node's `fileKey` and `nodeId`
2. **Get screenshot:** Call `get_screenshot` for visual reference
3. **Translate:** Map the Figma output (React + Tailwind) to Angular + Material + SCSS using the mappings below
4. **Validate:** Take a screenshot with Playwright to verify 1:1 visual parity before committing

## Figma Component to Angular Material Mapping

IMPORTANT: Always use these Angular Material equivalents. Never create custom components when Material provides one.

| Figma Component | Angular Material | Code |
|---|---|---|
| **Button / Filled** | Flat button | `<button mat-flat-button>Label</button>` |
| **Button / Outlined** | Stroked button | `<button mat-stroked-button>Label</button>` |
| **Button / Ghost** | Plain button | `<button mat-button>Label</button>` |
| **Input / Default** | Form field + input | `<mat-form-field><input matInput /></mat-form-field>` |
| **Input / Textarea** | Form field + textarea | `<mat-form-field><textarea matInput></textarea></mat-form-field>` |
| **Form Field** | Form field with label | `<mat-form-field><mat-label>Label</mat-label><input matInput /></mat-form-field>` |
| **Chip / Success** | Chip (custom class) | `<mat-chip class="chip-success">Label</mat-chip>` |
| **Chip / Primary** | Chip (custom class) | `<mat-chip class="chip-primary">Label</mat-chip>` |
| **Chip / Info** | Chip (custom class) | `<mat-chip class="chip-info">Label</mat-chip>` |
| **Chip / Default** | Chip | `<mat-chip>Label</mat-chip>` |
| **Search Input** | Form field with icon | `<mat-form-field><mat-icon matPrefix>search</mat-icon><input matInput placeholder="Search..." /></mat-form-field>` |
| **Select** | Form field + select | `<mat-form-field><mat-label>Label</mat-label><mat-select>...</mat-select></mat-form-field>` |
| **Nav Item / Active** | List item (active) | `<a mat-list-item routerLinkActive="active">...</a>` |
| **Nav Item / Default** | List item | `<a mat-list-item>...</a>` |
| **Sidebar** | Sidenav | `<mat-sidenav mode="side" opened>...</mat-sidenav>` |
| **App Bar** | Toolbar | `<mat-toolbar>...</mat-toolbar>` |

## Figma Variable to CSS Token Mapping

### Colors

| Figma Variable | CSS Token |
|---|---|
| `color/primary` (#006b5e) | `var(--mat-sys-primary)` |
| `color/on-primary` (#ffffff) | `var(--mat-sys-on-primary)` |
| `color/primary-container` (#72f7de) | `var(--mat-sys-primary-container)` |
| `color/on-surface` (#191c1b) | `var(--mat-sys-on-surface)` |
| `color/on-surface-variant` (#6f7a77) | `var(--mat-sys-on-surface-variant)` |
| `color/surface` (#ffffff) | `var(--mat-sys-surface)` |
| `color/surface-variant` (#dbe6e2) | `var(--mat-sys-surface-variant)` |
| `color/outline-variant` (#e0dbea) | `var(--mat-sys-outline-variant)` |
| `color/success` (#19a185) | `var(--ds-color-success)` |
| `color/success-container` (#d8f4ed) | `var(--ds-color-success-container)` |
| `color/info` (#d18519) | `var(--ds-color-info)` |
| `color/info-container` (#fcefd8) | `var(--ds-color-info-container)` |

### Spacing (Figma subset — full scale in `frontend/CLAUDE.md`)

| Figma Variable | CSS Token |
|---|---|
| `spacing/1` (4px) | `var(--ds-spacing-1)` |
| `spacing/2` (8px) | `var(--ds-spacing-2)` |
| `spacing/3` (12px) | `var(--ds-spacing-3)` |
| `spacing/4` (16px) | `var(--ds-spacing-4)` |
| `spacing/5` (20px) | `var(--ds-spacing-5)` |
| `spacing/6` (24px) | `var(--ds-spacing-6)` |
| `spacing/8` (32px) | `var(--ds-spacing-8)` |

### Border Radius

| Figma Variable | CSS Token |
|---|---|
| `radius/md` (8px) | `var(--ds-radius-md)` |
| `radius/lg` (12px) | `var(--ds-radius-lg)` |

### Layout

| Figma Variable | CSS Token |
|---|---|
| `layout/card-padding` (24px) | `var(--ds-card-padding)` |

## Card Pattern

Cards in the design are not a Figma component — they share a consistent style but vary in content. Implement with:

```scss
.card {
  background: var(--mat-sys-surface);
  border: 1px solid var(--mat-sys-outline-variant);
  border-radius: var(--ds-radius-lg);
  padding: var(--ds-card-padding);
}
```

Do NOT use `mat-card` — the design uses a simpler card style than Material's card component.

## Chip Styling

Chips use semantic colors not in Material's default palette. Style them with `--ds-color-*` tokens:

```scss
.chip-success {
  background: var(--ds-color-success-container);
  color: var(--ds-color-success);
}

.chip-primary {
  background: var(--mat-sys-primary-container);
  color: var(--mat-sys-primary);
}

.chip-info {
  background: var(--ds-color-info-container);
  color: var(--ds-color-info);
}
```

## Layout Constants

| Element | Height | Notes |
|---|---|---|
| App bar | 56px | Material toolbar default |
| Sidebar width | 240px | Fixed width |
| Sidebar logo area | 56px | Match app bar height |
| Table header | ~44px | — |
| Table row | ~48px | Material table default density |
| Filter/toolbar bar | ~56px | Enough for inputs + padding |
| Footer | ~40px | — |
| Page padding | 24px all sides | `var(--ds-card-padding)` or `var(--ds-spacing-6)` |
| Section gap | 24px | `var(--ds-spacing-6)` |

## Asset Handling

- IMPORTANT: If the Figma MCP server returns a localhost source for an image or SVG, use that source directly
- IMPORTANT: Do NOT import new icon packages — use Angular Material's `mat-icon` with Material Design Icons
- Store downloaded assets in `frontend/src/assets/`

## Styling Rules

All generated code MUST follow the mandatory styling rules in `frontend/CLAUDE.md` > Design Tokens & Styling. Key points: no hardcoded spacing/colors/typography — use `--ds-*` and `--mat-sys-*` tokens exclusively. See that file for the full token scale, allowed exceptions, and token file locations.
