# 001 - Walking skeleton

## Goal
Prove the architecture end to end before any business logic: a request goes through
the gateway to a service that reads from Postgres, everything started by docker compose.

## Scope
- Modules: `api-gateway`, `task-service`
- `task-service` exposes `GET /api/ping` returning `{"status":"ok","db":"<postgres version>"}`,
  where `db` is the full `SELECT version()` string, queried over R2DBC. DB failure → 503.
- One Flyway migration creating an empty table `skeleton`; Flyway uses JDBC at startup only, no pool (ADR 002).
- Gateway (Spring Cloud Gateway, ADR 001) routes `/api/**` to `task-service`.
- Only the gateway is published on the host.

## Acceptance criteria
- [ ] `docker compose down -v && docker compose up --build` starts everything from scratch with no errors.
- [ ] `curl localhost:8080/api/ping` → 200 with the Postgres version, through the gateway.
- [ ] Startup logs show the migration applied before the web server starts.
- [ ] No JDBC pool is created at runtime (`pg_stat_activity` shows only R2DBC connections after a request).
- [ ] `POST /api/ping` → 405.
- [ ] `docker compose ps` shows no published port for Postgres or `task-service`.
- [ ] `mvn test` passes with one slice test for `/api/ping` (200, 405, 503).
- [ ] Versions used are the latest GA and compatible; this spec lists each one with its source.
- [ ] README has running instructions.

## Versions
Checked on 2026-10-24. "Latest GA and compatible" means the newest GA release that the
other pinned versions support.

| Component | Version | Source |
|---|---|---|
| Java | 25 (Temurin JRE image `eclipse-temurin:25-jre-alpine`) | CLAUDE.md; Docker Hub tags |
| Maven (build image) | 3.10.0 (`maven:3.10.0-eclipse-temurin-25-alpine`) | Maven Central `apache-maven` metadata; Docker Hub tags |
| Spring Boot | 4.1.2 | start.spring.io metadata (default/latest GA; 4.2.0 is RC1) |
| Spring Cloud | 2025.1.4 (Gateway 5.0.4) | start.spring.io `bom-ranges`: supports Boot >=4.0.0 <4.2.0-M1; 2026.0.0 is RC1 |
| PostgreSQL | 18.6 (`postgres:18.6-alpine`) | postgresql.org/versions.json; 19 is RC1 |
| r2dbc-postgresql | 1.1.3.RELEASE | Maven Central metadata (managed by Boot) |
| r2dbc-pool | 1.0.2.RELEASE | Maven Central metadata (managed by Boot) |
| PostgreSQL JDBC | 42.7.14 | Maven Central metadata (managed by Boot) |
| Flyway | 12.4.0 | Managed by Boot 4.1.2. Maven Central has 13.11.1, not used: a major ahead of what Boot supports |

## Out of scope
- Authentication, users, tasks, Karate, Postman, OpenAPI, gateway resilience, gateway tests.

## Open questions
- (none yet)
