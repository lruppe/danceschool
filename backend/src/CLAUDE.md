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