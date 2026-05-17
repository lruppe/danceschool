# Dance School

Multi-tenant B2B SaaS for dance school management. Each school is a tenant; owners and teachers manage classes, students, enrollments, payments, and partner matching from a single admin portal.

- **Live app (WIP):** https://app.aquisebaila.ch (**free hosting with warm-up time on render**, no test data available)
- **API:** https://api.aquisebaila.ch

## What it does

The current product (Phase 1) is an admin portal for dance schools:

- **School & member management** — owners create a school, invite teachers, configure pricing plans
- **Course lifecycle** — `DRAFT → OPEN → RUNNING → FINISHED`, derived from publish/start/end dates
- **Enrollment workflow** — covers `PENDING_PAYMENT`, `PENDING_APPROVAL`, `CONFIRMED`, `WAITLISTED`, `REJECTED`
- **Capacity & role balancing** — solo courses use simple capacity; partner courses (Salsa, Bachata, …) track lead/follow distribution with FIFO waitlisting when imbalance exceeds a threshold
- **Level-gated approval** — `INTERMEDIATE+` courses require owner approval when a student's recorded dance level is missing or too low
- **Manual payment tracking** — owners mark payments received; Stripe integration is architected behind a single seam for later activation

See `docs/GLOSSARY.md` for the ubiquitous language and `docs/TESTING_WORKFLOWS.md` for end-to-end business scenarios.

## Tech stack

| Layer | Stack |
|---|---|
| Frontend | Angular 21 (standalone components, signals), Angular Material, SCSS design tokens |
| Backend | Spring Boot 3.5 (Java 21), Spring Security, Spring Data JPA, Liquibase, H2 (dev) |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Auth (dev) | Form login + session cookie; in-memory users seeded on startup |
| Auth (prod) | Firebase JWT (Google sign-in), stateless OAuth2 resource server |
| Testing | JUnit + MockMvc integration tests, Vitest + Playwright on the frontend, Testcontainers (`*IT.java`) for external services |
| CI | GitHub Actions — backend `mvn verify`, frontend `npm test` + `ng build` on every PR |

## Architecture

Monorepo with two independent builds — no Maven/npm coupling:

```
danceschool/
├── frontend/          Angular 21 SPA
├── backend/           Spring Boot REST API
├── docs/              Glossary, testing workflows, tech debt
└── .github/           CI, CodeQL, Dependabot
```

**Multi-tenancy** is the central design constraint. Every domain entity belongs to a `School`, and the backend enforces isolation mechanically:

- Repository methods take `schoolId` alongside any entity ID (e.g. `findByIdAndSchoolId`)
- ArchUnit tests fail the build if a `@TenantScoped` controller reaches an unscoped finder, skips `@PreAuthorize`, or talks to a repository directly
- Every `@RestController` is either `@TenantScoped` or on an explicit open-controller allowlist
- Per-request `TenantContextFilter` populates MDC with `[schoolId=… userId=…]` for all logging

**Package-by-feature** on the backend: each feature owns its Entity, Repository, Service, Controller, and DTOs. Shared infrastructure (`shared/error`, `shared/logging`, `shared/security`) is the only cross-cutting layer features may depend on.

**Design tokens** on the frontend: hardcoded spacing, colors, and typography are forbidden — all styles route through `--ds-*` and `--mat-sys-*` CSS variables defined in `frontend/src/styles/`.

## Roadmap

- **Phase 1 (now)** — Admin portal for owners and teachers. Manage classes, students, enrollments, partner matching, manual payments.
- **Phase 1.5 (pilot)** — Public student-booking website. Students browse pilot schools and enroll with name/email/phone; no accounts.
- **Phase 2** — Native student app (Android/iOS). Social login (Google, Apple), discovery, community events feed.
- **Phase 3** — Online payments via Stripe (Mobile SDK + Apple/Google Pay), multi-tenant payouts via Stripe Connect.

Phase 1 deliberately makes simplifying assumptions (single school per user, `OWNER == TEACHER`, implicit school in `/me` URLs, no student login) that later phases will lift. Authorization is designed so these collapses can be widened without reshaping endpoints. Full details in `.claude/rules/product-roadmap.md`.

## AI-assisted development workflow

The project is built extensively with Claude Code. The workflow is codified in `.claude/commands/` so every feature follows the same path:

1. **Design in Figma Make** — quick interactive prototype of a screen or flow
2. **Promote to Figma components** — consolidate primitives (Button, Chip, Form Field, …) and page compositions in the [design file](https://www.figma.com/design/uQoQs1ZZKO8fGIwFsaLStB/Dance-School-Admin)
3. **Write a PRD** (`/write-a-prd`) — a single GitHub issue capturing problem, solution, user stories, and design references (Figma links, not embedded screenshots)
4. **Break into vertical-slice issues** (`/prd-to-issues`) — each issue is a tracer bullet through schema → API → UI → tests, marked HITL or AFK and linked as a sub-issue of the parent PRD
5. **Implement** (`/start-issue <N>`) — automated lifecycle: worktree + branch, plan mode for non-trivial work, code + tests, visual verification, code review
6. **Verify**
   - **Visual:** Playwright at 1440×900 against a real Angular dev server + real Spring Boot backend, screenshotting every affected page
   - **Design parity:** `get_design_context` + `get_screenshot` from Figma compared against the implementation
   - **Code review:** an automated `code-reviewer` agent reads the diff against project conventions, the originating issue, and the multi-tenant authz rules; returns Blockers / Suggestions / Nits
7. **Ship** — commit, rebase, push, PR with summary + test plan, watch CI, squash-merge after green
8. **Deploy** — automatic on Render via webhooks on `main`

The full step-by-step lives in `.claude/commands/start-issue.md`.
 

## Security

- **Dependabot** — weekly version updates for npm (`frontend/`), Maven (`backend/`), and GitHub Actions
- **CodeQL** — code scanning configured in `.github/workflows/codeql.yml`
- **Secret scanning** — enabled at the repository level on GitHub
- **CI gate** — backend `mvn verify` (Surefire + Failsafe, including Testcontainers) and frontend `npm test` + `ng build` must pass before merge
- **ArchUnit authz tests** — multi-tenant isolation and authorization rules are enforced at build time, not just at runtime

## Getting started

**Backend** (Java 21):

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. The H2 in-memory database resets on every restart; `DevDataSeeder` seeds two schools with separate owners.

**Frontend** (Node 22):

```bash
cd frontend
npm install
npx ng serve
```

Runs on `http://localhost:4200`. The Angular dev server proxies `/api` to the backend via `proxy.conf.json`.

**Dev credentials:**

| User | School |
|---|---|
| `owner@test.com` / `password` | School 1 |
| `owner2@test.com` / `password` | School 2 |

**Further reading:**

- `CLAUDE.md` — repo conventions and the issue workflow
- `frontend/CLAUDE.md` — Angular conventions and design tokens
- `backend/src/CLAUDE.md` — Spring Boot architecture and authz guardrails
- `docs/GLOSSARY.md` — domain language
- `docs/TESTING_WORKFLOWS.md` — manual end-to-end scenarios
