# Tech Debt

- Public URL on R2 Cloudflare S3 Bucket
- Check dev tools endpoints (available in prod?)

## Course Domain

- **Teachers field** — `Course.teachers` is a plain String (e.g. "Maria, Carlos"). Needs to become a `@ManyToMany` relation to `SchoolMember` (role=TEACHER) when teacher signup/invite flow is built.
- **Server-side pagination** — all table endpoints currently return the full list. Payments table will need backend `Pageable` support as transaction volume grows (rule of thumb: >500 rows).
- **Mobile table layout** — tables are desktop-only. Needs a design decision (horizontal scroll, card layout, or hidden columns) before mobile support.
- **Waitlist auto-promotion** — when a committed seat is freed, the next waitlisted enrollment should be promoted to `PENDING_PAYMENT` (per role for partner courses, FIFO). Currently not implemented because no flow frees seats: there is no cancel/withdraw endpoint and no rejection path for confirmed enrollments. Design alongside the first flow that frees a seat (likely a student cancellation or admin refund flow). Documented as a known limitation in `docs/TESTING_WORKFLOWS.md`.

## Demo Mode

- **Tear down `app.demo.enabled` wiring once real signup ships** (#386) — the live deployment is branded as "Demo Dance School" and auto-provisions a populated demo school for every authenticated Firebase user that doesn't already own one (via `DemoSchoolSeeder`'s `@TransactionalEventListener` on `UserAuthenticatedEvent`, which fires per authenticated request — a cheap `hasSchoolByMember` check is the steady-state cost). When the real onboarding flow exists, remove: the `app.demo.enabled` property + its `application-prod.yaml` value, the listener method + flag in `DemoSchoolSeeder`, the `UserAuthenticatedEvent` publish in `UserService`, `DemoSchoolSeederTest`, the `isDemo` frontend env flag, and the DEMO chip + brand prefix in `shell.html` / `app.ts`. `DemoSchoolSeeder.seedDemoSchoolFor` itself can stay (still used by `DevDataSeeder` for local dev).
- **Demo seeding has no abuse cap** (#386) — any Google account holder can trigger ~30 DB writes by signing in once (1 school + 7 courses + 7 students + ~18 enrollments). Acceptable while the deployment is essentially a marketing demo, but if it stays open longer than expected, add a per-IP/per-day cap or a TTL job that purges demo schools older than N days. Re-evaluate alongside the teardown above.

## Dashboard

- **Demo dashboard needs proper design and a backend aggregation endpoint** — the current dashboard is a placeholder: three stat cards (active students, active courses, total revenue) plus an Open Payments action card. It fan-outs to `/api/payments/me`, `/api/students`, `/api/courses/me` and aggregates client-side. Two gaps to close before it's a real dashboard: (1) UX/design — owners need to confirm which metrics and actions actually drive their daily workflow (e.g. pending approvals, students missing dance levels, upcoming sessions, waitlist movement) and how the page should be laid out; (2) backend — once the metric set is decided, add a `GET /api/dashboard/me` endpoint that returns the aggregates in one call (pending approvals especially require this — enrollments are scoped per course today, so aggregating across courses needs either N+1 calls or a new query).

## Tenant Isolation

- **TenantScopedRepository for sensitive entities** (#174) — tenant isolation currently relies on services remembering to call school-scoped repository methods. All repositories extend `JpaRepository`, which exposes unscoped methods (`findById`, `findAll`). When the first strictly tenant-scoped entity is added (likely Payment), introduce a `TenantScopedRepository` base interface that extends `Repository` directly (not `JpaRepository`) and only exposes write operations. Enforce with `@StrictTenantIsolation` annotation + ArchUnit test. Course & School stay on `JpaRepository` since they need cross-tenant reads for the Phase 2 student app.
