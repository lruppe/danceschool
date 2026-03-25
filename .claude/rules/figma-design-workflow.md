# Figma Design Workflow

## Figma file links

- **Design file** (source of truth for components and layouts): https://www.figma.com/design/uQoQs1ZZKO8fGIwFsaLStB/Dance-School-Admin
- **Make file** (generated running app — use as reference for how the design should look and behave): https://www.figma.com/make/1UzKcbslBrvkxIZFiygawy/Dance-School-Admin

## Comparing Figma Make files to design files

- Figma Make generates a running React+MUI app from the design. Use its preview URL to see how the design *should* look with proper component rendering.
- To pull a Make file's code: use `get_design_context` with the Make fileKey — it returns resource links. Read key files (pages, layout, theme) via `ReadMcpResourceTool` with server `plugin:figma:figma`.
- The Make file's MUI theme and page components reveal the intended spacing, border-radius, and component density. Use these as the reference, not the raw Figma frame dimensions.

## Component structure in Figma

- Reusable elements (Sidebar, App Bar, page content) must be **components**, not plain frames.
- Layout compositions (e.g., "App Layout - Students") should use **instances** of those components so changes propagate.
- Before building, inspect existing nodes with `use_figma` to check `node.type` — look for FRAME vs COMPONENT vs INSTANCE.
- When a page layout duplicates content instead of instancing, fix it: convert the standalone frame to a component (`figma.createComponentFromNode()`), then replace the duplicate with an instance.
- **Always check existing components before creating new elements.** Use `use_figma` to list components on the page, or inspect the component library below.

## Available component library

Always use these instead of creating inline frames:

**Primitives (component sets with variants):**
- **Button** — Variants: `Filled` (primary action), `Outlined` (secondary), `Ghost` (tertiary/add actions). Override the `Label` text.
- **Input** — Variants: `Default` (single-line), `Textarea` (multi-line). Override the `Value` text.
- **Chip** — Variants: `Success` (Active status), `Primary` (Bachata), `Info` (Salsa), `Default` (Inactive).
- **Nav Item** — Variants: `Active`, `Default`.

**Compound components:**
- **Form Field** — Label + Input instance. Override `Label` text and the nested Input's `Value`.
- **Search Input** — Search icon + placeholder text.
- **Select** — Dropdown with label + chevron.

**Layout components:**
- **Sidebar** — Logo area + divider + navigation items (Dashboard, Students, My School).
- **App Bar** — School name + avatar.

**Page components:**
- **Students Page**, **My School Page**, **My School Edit Page** — full page content, used as instances in App Layouts.

**Card pattern (not a component — intentional):**
Cards share a consistent style but vary too much in content to componentize: white fill, 12px corner radius, `rgba(0,0,0,0.08)` border, 24px padding.

## Spacing and density standards

- Table rows: ~48px (not 100px)
- Table headers: ~44px
- Filter/toolbar bars: ~56px (enough for inputs + padding)
- Footers: ~40px
- App bar: 56px
- Sidebar logo area: 56px (match app bar)
- Page section gap: 24px
- Page padding: 24px all sides
- Filter bars: use `counterAxisAlignItems = 'CENTER'` for vertical centering

## Incremental Figma editing workflow

1. **Inspect first** — use `get_metadata` to understand the structure, `use_figma` to read layout properties
2. **Fix structure** — ensure components and instances are wired up correctly
3. **Fix spacing** — resize frames, adjust padding/gaps
4. **Screenshot to verify** — use `get_screenshot` after each batch of changes
5. Update both standalone components AND check that instances reflect changes
