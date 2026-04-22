# Backend — Spring Boot

## Build & Run Commands

- **Build:** `./mvnw compile`
- **Run:** `./mvnw spring-boot:run`
- **Run all tests:** `./mvnw test`
- **Run a single test:** `./mvnw test -Dtest=ClassName#methodName`
- **Package:** `./mvnw package`

## Architecture

Spring Boot 3.5 application (Java 21) for managing a dance school. Uses Maven wrapper (`./mvnw`).

**Key stack:**
- Spring Web (REST API), Spring Data JPA (persistence), Spring Validation
- Spring Security + OAuth2 Resource Server (stateless Firebase JWT auth)
- Liquibase for database migrations (`src/main/resources/db/changelog/db.changelog-master.yaml`)
- H2 in-memory database (dev/test)
- Lombok for boilerplate reduction (configured as annotation processor in maven-compiler-plugin)
- SpringDoc OpenAPI for API docs (Swagger UI at `/swagger-ui.html`)
- Spring Boot Actuator (endpoints at `/actuator`)

**Base package:** `ch.ruppen.danceschool`

## Architectural Rules

### 1. Package-by-Feature
Each domain feature gets its own package under `ch.ruppen.danceschool.<feature>`. A feature package contains: Entity, Repository, Service, Controller, DTO(s). No shared `controller/`, `service/`, `repository/` packages.

### 2. Shared Infrastructure (`shared/`)
- Packages under `shared/` are cross-cutting infrastructure (e.g., `error/`, `logging/`, `security/`, `storage/`)
- Any feature may inject from `shared/` directly
- `shared/` packages must not depend on feature packages

### 3. Services Own All Business Logic
- Services contain all business logic and coordination — loading entities, orchestrating calls, converting to DTOs
- A service may inject another feature's service or repository when needed
- Avoid circular dependencies between services

### 4. Controllers Are Pure HTTP Adapters
- Controllers handle HTTP concerns only: request mapping, validation (`@Valid`), status codes
- A controller method calls **one** service method and returns the result — no orchestration, no entity handling
- Example: `return schoolService.updateSchool(userId, dto);` — the service owns the full flow

### 5. Minimal Lombok
- Use only what you need: `@Getter`, `@Setter`, `@NoArgsConstructor` for entities
- Do not use `@Data` on JPA entities (broken equals/hashCode with lazy loading)
- `@RequiredArgsConstructor` is mandatory for constructor injection in services/controllers

### 6. One DB Schema, Liquibase Migrations
- All features share one schema — no schema-per-feature separation
- Every schema change requires a Liquibase changeset; never modify entities without a corresponding migration

### 7. Error Handling
- Domain exceptions live in `shared/error/`: e.g. `ResourceNotFoundException` (404), `DomainRuleViolationException` (409)
- `GlobalExceptionHandler` maps exceptions to RFC 9457 ProblemDetail responses
- Bean Validation on DTOs with `@Valid` in controllers; validation errors return structured `fieldErrors`
- Do not use `IllegalArgumentException` for domain rule violations — use `DomainRuleViolationException`

### 8. Logging
- AOP aspects in `shared/logging/` handle general logging (controllers, services), expand if necessary
- Annotate service methods that represent domain events (create, update, delete) with `@BusinessOperation(event = "EventName")` — the `BusinessLoggingAspect` logs these on a dedicated `business` logger in `event=Name k=v` format
- Every log line carries `[schoolId=… userId=…]` via MDC (populated per-request by `TenantContextFilter`) — do **not** also pass `schoolId`/`userId` as kv args; it's redundant

## Security

### Auth modes (controlled by `app.security.dev-auth` property)
- **Dev** (`dev-auth: true`, default): form login with session auth. `DevDataSeeder` seeds two owners with separate schools on startup. `@AuthenticationPrincipal AuthenticatedUser` works identically to prod.
- **Prod** (`dev-auth: false`): stateless Firebase JWT. First request auto-creates the user from token claims.

### Authorization
- Roles are **scoped to a school**, not global — stored in `school_member` table (OWNER, TEACHER)
- A user with no memberships is in the "needs onboarding" state

### Configuration (env vars)
- `CORS_ALLOWED_ORIGINS` — comma-separated (default: `http://localhost:4200`)
- `DEV_AUTH` — `true` for form login, `false` for Firebase JWT
- `FIREBASE_PROJECT_ID` — default: `dance-school-ch`

### Authz guardrails (ArchUnit)

Admin-side authz rules are enforced mechanically by `src/test/java/ch/ruppen/danceschool/archunit/AdminAuthzArchTest.java`. Read it before adding or touching a controller — the test fails the build with loud, multi-line banners that tell you exactly what to change.

Rules enforced:
1. **`@TenantScoped` controllers don't reach unscoped finders** — no transitive call from a `@TenantScoped` class into `findById`/`existsById`/`deleteById`/`getReferenceById` on `CourseRepository`/`StudentRepository`/`EnrollmentRepository`. Use the `*AndSchoolId` variant.
2. **Every `@TenantScoped` method has `@PreAuthorize`** — directly or via class-level annotation.
3. **Id-taking repo methods returning a tenant entity take `schoolId` too** — prevents adding new id-scoped finders that bypass tenant isolation.
4. **`@TenantScoped` controllers don't depend on `*Repository` directly** — data access goes through a service.
5. **Every `@RestController` is either `@TenantScoped` or on the open-controller allowlist** — forces an explicit authz classification when a new controller appears.
6. **`@PreAuthorize("@schoolAuthz.hasMembership()")` implies `@TenantScoped`** — catches the membership-gate SpEL copy-pasted onto an open controller.

**Adding a new admin controller:** annotate the class `@TenantScoped` and apply `@PreAuthorize("@schoolAuthz.hasMembership()")` at the class level.

**Adding a new open controller** (e.g., webhooks, Phase 1.5 `/api/public/**`): add the class simple name to `AdminAuthzArchTest.OPEN_CONTROLLER_ALLOWLIST` with a Javadoc note on the controller explaining why it's open.

## Testing

**When to write tests:**
- New API endpoints — integration test (`@SpringBootTest` + `MockMvc`) covering happy path, validation errors, and auth requirements
- Domain logic in services with business rules beyond simple CRUD
- Custom `@Query` methods
- Tenant isolation — only when an endpoint accepts an ID/parameter that could reference another tenant's data. `/me` pattern endpoints are isolated by design.

**Style:** Integration tests over unit tests. Real Spring context, real database. Match existing pattern (`@SpringBootTest` + `MockMvc` + `EntityManager` for setup).

### Test naming convention (Surefire vs Failsafe)

- `*Test.java` — runs during `mvn test` (Surefire). Fast tests, no Docker required. Use for all standard unit and integration tests.
- `*IT.java` — runs during `mvn verify` (Failsafe). Slow tests requiring Docker (e.g., testcontainers). CI runs `mvn verify` so these always execute in the pipeline.

### Testcontainers (for external service integration tests)

Use testcontainers with LocalStack when testing code that interacts with external services (e.g., AWS S3/Cloudflare R2). These tests must be named `*IT.java` so they only run during `mvn verify` (requires Docker). See `CloudflareR2ImageStorageServiceIT` for the pattern. Dependencies: `org.testcontainers:localstack` and `org.testcontainers:junit-jupiter`.