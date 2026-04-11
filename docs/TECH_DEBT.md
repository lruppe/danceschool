# Tech Debt

- Public URL on R2 Cloudflare S3 Bucket
- String config for Enums
- Proper ControllerAdvice

## Course Domain

- **Teachers field** — `Course.teachers` is a plain String (e.g. "Maria, Carlos"). Needs to become a `@ManyToMany` relation to `SchoolMember` (role=TEACHER) when teacher signup/invite flow is built.
- **Enrollment counts** — `Course.enrolledStudents` is a stored integer, seeded with sample data. Needs to be computed from actual enrollment records when student enrollment is built.
- **Server-side pagination** — all table endpoints currently return the full list. Payments table will need backend `Pageable` support as transaction volume grows (rule of thumb: >500 rows).
- **Mobile table layout** — tables are desktop-only. Needs a design decision (horizontal scroll, card layout, or hidden columns) before mobile support.

## Tenant Isolation

- **TenantScopedRepository for sensitive entities** (#174) — tenant isolation currently relies on services remembering to call school-scoped repository methods. All repositories extend `JpaRepository`, which exposes unscoped methods (`findById`, `findAll`). When the first strictly tenant-scoped entity is added (likely Payment), introduce a `TenantScopedRepository` base interface that extends `Repository` directly (not `JpaRepository`) and only exposes write operations. Enforce with `@StrictTenantIsolation` annotation + ArchUnit test. Course & School stay on `JpaRepository` since they need cross-tenant reads for the Phase 2 student app.
