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
- Spring Security (session-based auth, single in-memory admin user)
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
- Single admin user (`dance_admin`) configured via Spring Security's in-memory user properties
- `POST /api/auth/login` accepts `{username, password}`, authenticates via `AuthenticationManager`, creates HTTP session
- Session cookie (`JSESSIONID`) maintains authentication across requests
- `GET /api/auth/me` returns the authenticated user with their school memberships
- `POST /api/auth/logout` invalidates the session

### Authorization Model
- Roles are **scoped to a school**, not global — stored in `school_member` table
- `SchoolMember` links a `User` to a `School` with a `MemberRole` (OWNER, USER)
- A user with no memberships is in the "needs onboarding" state
- `GET /api/auth/me` returns the user with their memberships — frontend uses this to route

### Configuration (via env vars)
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins (default: `http://localhost:4200`)

### CSRF
- Disabled for now (single-admin app). Re-enable when integrating Auth0 or adding multi-user support.

### Key classes in `shared/security/`
- `SecurityConfig` — filter chain, CORS, session auth, authorization rules
- `AuthenticatedUser` — principal record (userId, email) available via `@AuthenticationPrincipal`
- `AppSecurityProperties` — CORS configuration