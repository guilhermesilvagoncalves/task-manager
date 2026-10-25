# ADR 002 - Flyway for schema migrations

## Context
`task-service` accesses Postgres reactively over R2DBC. The schema needs versioned,
repeatable migrations. Flyway only supports JDBC, which is blocking. The project rule
allows blocking work only at startup or on `boundedElastic`.

## Decision
Use Flyway, running once at startup over a plain JDBC connection with no pool:
- Depend on `spring-boot-flyway` rather than `spring-boot-starter-flyway`, because the
  starter pulls in HikariCP.
- Configure it with `spring.flyway.url`, so Boot builds an unpooled `SimpleDriverDataSource`.
- Migrations run before the web server starts, so no request waits on them.

## Alternatives
- **r2dbc-migrate** (community): versioned migrations over R2DBC, fully non-blocking.
  Smaller community, fewer features and databases, depends on a single main maintainer.
- **`spring.sql.init` with R2DBC** (built into Boot): runs `schema.sql` reactively, but has
  no versioning or history, so it is unsafe for evolving the schema.
- **Liquibase**: JDBC only as well, so it has the same trade-off as Flyway with no benefit here.

## Consequences
- Industry-standard tooling, well supported by Spring Boot.
- Two Postgres drivers on the classpath (JDBC for Flyway, R2DBC for the app), and the
  database URL is configured twice.
- Blocking happens only at startup; no JDBC connection stays open at runtime.
- The Flyway version is the one Boot manages (12.4.0), not the newest release (13.x).
