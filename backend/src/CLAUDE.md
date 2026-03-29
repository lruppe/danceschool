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

### Authentication Flow (Firebase JWT)
- **Stateless JWT authentication** — no sessions, no cookies
- Frontend authenticates via Firebase SDK (Google sign-in), sends Firebase ID token as `Authorization: Bearer <token>` on every request
- Backend validates JWTs via Spring Security OAuth2 Resource Server against Firebase's issuer
- `FirebaseJwtAuthenticationConverter` extracts Firebase UID, email, and name from JWT claims, then looks up or auto-creates an `app_user` record
- `GET /api/auth/me` returns the authenticated user with their school memberships (resolved from JWT)
- No login/logout endpoints — authentication is handled entirely client-side by Firebase SDK

### User Auto-Provisioning
- First request with a valid Firebase JWT auto-creates an `app_user` from token claims (UID, email, name)
- Subsequent requests reuse the existing user (looked up by `firebase_uid`)
- The `app_user.firebase_uid` column is the unique identifier linking Firebase to the app's user model

### Authorization Model
- Roles are **scoped to a school**, not global — stored in `school_member` table
- `SchoolMember` links a `User` to a `School` with a `MemberRole` (OWNER, USER)
- A user with no memberships is in the "needs onboarding" state
- `GET /api/auth/me` returns the user with their memberships — frontend uses this to route

### Configuration (via env vars)
- `CORS_ALLOWED_ORIGINS` — comma-separated allowed origins (default: `http://localhost:4200`)
- `FIREBASE_ISSUER_URI` — Firebase JWT issuer URI (default: `https://securetoken.google.com/danceschool-dev`)
- `FIREBASE_PROJECT_ID` — Firebase project ID (default: `danceschool-dev`)

### CSRF
- Disabled — stateless JWT auth does not need CSRF protection

### Key classes in `shared/security/`
- `SecurityConfig` — filter chain, CORS, stateless JWT auth, authorization rules
- `FirebaseJwtAuthenticationConverter` — converts validated JWT to `AuthenticatedUser` principal, handles user auto-provisioning
- `FirebaseAuthenticationToken` — custom `JwtAuthenticationToken` that carries `AuthenticatedUser` as principal
- `AuthenticatedUser` — principal record (userId, email) available via `@AuthenticationPrincipal`
- `AppSecurityProperties` — CORS configuration

### Testing
- Tests use a mock `JwtDecoder` (via `TestSecurityConfig`) to avoid real Firebase OIDC discovery
- Auth integration tests verify JWT validation, user auto-provisioning, and endpoint behavior
- Existing controller tests use `authentication()` post processor with `AuthenticatedUser` principal