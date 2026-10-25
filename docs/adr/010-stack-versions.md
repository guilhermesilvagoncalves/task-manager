# ADR 010 - Stack versions

## Context
CLAUDE.md requires Java 25 and, for every dependency, the latest stable (GA) version with active
support; never RC or milestone. Versions must also be compatible with each other.

## Decision
"Latest GA and compatible" means the newest GA release that the other pinned versions support.
Spring Boot drives the rest: its BOM manages most libraries, and Spring Cloud and springdoc follow
the Boot line they support. Re-checked on 2026-10-25 against Maven Central, postgresql.org and Docker Hub.

| Component | Version | Why not newer |
|---|---|---|
| Java (Temurin JRE image) | 25 (`eclipse-temurin:25-jre-alpine`) | Required by CLAUDE.md |
| Maven (build image) | 3.10.0 (`maven:3.10.0-eclipse-temurin-25-alpine`) | 4.0.0 is still RC |
| Spring Boot | 4.1.2 | 4.2.0 is RC1 |
| Spring Cloud | 2025.1.4 (Gateway 5.0.4) | 2026.0.0 is RC1 and targets Boot 4.2 |
| springdoc-openapi | 3.2.0 | Latest GA |
| PostgreSQL | 18.6 (`postgres:18.6-alpine`) | 19 is not GA |
| Flyway | 12.4.0 (managed by Boot) | 13.x is a major ahead of what Boot 4.1 supports |
| r2dbc-postgresql, r2dbc-pool, PostgreSQL JDBC, Spring Security, Testcontainers | Managed by Boot 4.1.2 | Boot BOM |
| Karate (`karate-junit6`) | 2.1.4 | Latest GA |

## Alternatives
- **Newest of everything, including RC/milestones**: forbidden by CLAUDE.md.
- **Newest GA of each library on its own** (e.g. Flyway 13): not supported by Boot's
  auto-configuration; risks runtime incompatibilities.
- **Older LTS lines** (e.g. Boot 3.x, Java 21): stable, but not what CLAUDE.md asks for.

## Consequences
- Versions must be re-checked on delivery and when a new Boot GA is released; the date above shows the last check.
- Overriding a Boot-managed version needs a reason recorded here.
