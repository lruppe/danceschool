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
- Spring Security + OAuth2 Client (Google, GitHub social login)
- JWT authentication (jjwt library, HMAC-SHA256, HTTP-only cookie)
- Liquibase for database migrations (`src/main/resources/db/changelog/db.changelog-master.yaml`)
- H2 in-memory database (dev/test)
- Lombok for boilerplate reduction (configured as annotation processor in maven-compiler-plugin)
- MapStruct for entity/DTO mapping (`@Mapper(componentModel = "spring")`, package-private, per slice)
- SpringDoc OpenAPI for API docs (Swagger UI at `/swagger-ui.html`)
- Spring Boot Actuator (endpoints at `/actuator`)

**Base package:** `ch.ruppen.danceschool`

## Architectural Rules

### 1. Package-by-Feature (Vertical Slices)
Each domain feature gets its own package under `ch.ruppen.danceschool.<feature>`. A slice contains: Entity, Repository, Service, Controller, DTO(s). No shared `controller/`, `service/`, `repository/` packages.

### 2. Service is the Public API of a Slice
- Other slices **must inject the Service**, never the Repository or Entity directly
- The Repository is internal to its slice — no outside access
- Entities should not leak across slice boundaries; use DTOs for cross-slice communication

### 3. One-Way Service Dependencies Only
- A service may inject another slice's service (e.g., `CourseService -> SchoolService`)
- **Circular service dependencies are forbidden** — if A -> B, then B -> A is not allowed

### 4. Use-Case Classes for Cross-Slice Orchestration
- When a use case requires coordinating multiple slices and would cause a circular dependency, create a **Use-Case class** (e.g., `CreateSchoolWithCourseUseCase`)
- The use-case class lives in the package of the initiating feature
- It injects the required services and orchestrates the flow
- Controllers delegate to use-case classes — they contain no business logic themselves

### 5. Controllers Are Thin
- Controllers handle HTTP concerns only: request mapping, validation (`@Valid`), status codes, response wrapping
- Business logic lives in Services or Use-Case classes
- Controllers inject either a Service (simple CRUD) or a Use-Case class (cross-slice orchestration)

### 6. Minimal Lombok
- Use only what you need: `@Getter`, `@Setter`, `@NoArgsConstructor` for entities
- Do not use `@Data` on JPA entities (broken equals/hashCode with lazy loading)
- Do not add `@Builder`, `@AllArgsConstructor`, etc. unless actually needed
- `@RequiredArgsConstructor` is fine for constructor injection in services/controllers

### 7. One DB Schema, Liquibase Migrations
- All features share one schema — no schema-per-feature separation
- Every schema change requires a Liquibase changeset; never modify entities without a corresponding migration

### 8. Error Handling
- Domain exceptions live in `shared/error/`: `ResourceNotFoundException` (404), `DomainRuleViolationException` (409)
- `GlobalExceptionHandler` maps exceptions to RFC 9457 ProblemDetail responses
- Bean Validation on DTOs with `@Valid` in controllers; validation errors return structured `fieldErrors`
- Do not use `IllegalArgumentException` for domain rule violations — use `DomainRuleViolationException`

### 9. Logging
- AOP aspects in `shared/logging/` handle all logging (controllers, services)
- No manual logging in slice code — the aspects cover entry, exit, duration, and errors
- Use `@Slf4j` when adding loggers to new cross-cutting classes
- No PII at INFO level (class/method names and durations only, full payloads at DEBUG)

## Security Architecture

### Authentication Flow
- Backend acts as OAuth2 Client — handles the entire Google/GitHub login redirect flow
- After OAuth2 success, backend mints its own JWT (HMAC-SHA256, 7-day expiry) containing `userId` and `email`
- JWT is set as an `AUTH_TOKEN` HTTP-only cookie (Secure+SameSite=None in prod, Lax in dev — controlled by `SECURE_COOKIES` env var)
- On subsequent requests, `JwtAuthFilter` reads the cookie, validates the JWT, and sets `SecurityContext`
- Sessions are `IF_REQUIRED` (used briefly during OAuth2 redirect dance, not for ongoing auth)

### Authorization Model
- Roles are **scoped to a school**, not global — stored in `school_member` table
- `SchoolMember` links a `User` to a `School` with a `MemberRole` (OWNER, USER)
- A user with no memberships is in the "needs onboarding" state
- `GET /api/auth/me` returns the user with their memberships — frontend uses this to route

### Dev Login
- `POST /api/dev/login` with `{"email": "..."}` — available only when `prod` profile is NOT active
- Creates/finds a user and sets the same JWT cookie as OAuth2 would
- Use for local development and Playwright E2E tests
- CSRF is disabled for `/api/dev/**`

### Configuration (via env vars)
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — Google OAuth2 credentials
- `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` — GitHub OAuth credentials
- `JWT_SECRET` — HMAC-SHA256 signing key (min 32 bytes)
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins (default: `http://localhost:4200`)
- `FRONTEND_URL` — redirect target after OAuth2 login (default: `http://localhost:4200`)
- `SECURE_COOKIES` — set `true` in prod for Secure+SameSite=None cookies (default: `false` for dev)

### CSRF
- Uses `CookieCsrfTokenRepository.withHttpOnlyFalse()` — Angular reads the `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` header automatically
- Spring Security 6+ defers CSRF token loading — a `csrfCookieFilter` eagerly loads it so the cookie is set on every response
- CSRF is disabled for `/api/dev/**` (dev-only endpoints)

### Key classes in `shared/security/`
- `SecurityConfig` — filter chain, CORS, CSRF, OAuth2, authorization rules
- `JwtUtil` — sign/validate JWTs
- `JwtAuthFilter` — reads JWT from cookie, sets SecurityContext
- `JwtCookieUtil` — set/clear/read the AUTH_TOKEN cookie
- `OAuth2LoginSuccessHandler` — creates user, mints JWT, redirects to frontend
- `AuthenticatedUser` — principal record (userId, email) available via `@AuthenticationPrincipal`

## Database Migrations

All schema changes go through Liquibase. Add new changesets to files included from `db.changelog-master.yaml`. Never modify JPA entities without a corresponding migration.
