# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- Liquibase for database migrations (`src/main/resources/db/changelog/db.changelog-master.yaml`)
- H2 in-memory database (dev/test)
- Lombok for boilerplate reduction (configured as annotation processor in maven-compiler-plugin)
- SpringDoc OpenAPI for API docs (Swagger UI at `/swagger-ui.html`)
- Spring Boot Actuator (endpoints at `/actuator`)

**Base package:** `ch.ruppen.danceschool`

## Database Migrations

All schema changes go through Liquibase. Add new changesets to files included from `db.changelog-master.yaml`. Never modify JPA entities without a corresponding migration.
