# ADR 007 - Database per service on a shared Postgres instance

## Context
`auth-service` needs to store users (spec 002). `task-service` already uses the `tasks`
database on the single Postgres instance from spec 001. Services must stay loosely coupled,
but the current load is low: one Postgres instance handles both services with room to spare.

## Decision
Each service gets its own database and database user on the same Postgres instance:
- `auth` database and `auth` user, created by `docker/postgres/init-auth-db.sql`
  (mounted in `/docker-entrypoint-initdb.d`).
- Each service's Flyway owns only its own schema. No cross-database queries, no shared tables.
- Connection comes only from `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

## Alternatives
- **Schema `auth` inside the `tasks` database**: no init script, but both services share one
  database and its credentials by default, so isolation depends on grants.
- **One Postgres instance per service**: strongest isolation, independent scaling and failures.
  More containers and memory than the current load justifies.
- **Shared tables**: couples the services through the schema. Rejected.

## Consequences
- One container, low resource use.
- Logical isolation only: services share CPU, memory, connections and a single point of failure.
- The init script runs only on an empty volume; existing setups need `docker compose down -v` once.
- The Postgres healthcheck checks only `tasks`; if the init script fails, `auth-service` fails
  later, at Flyway startup.
- When load or isolation needs grow, moving to a dedicated instance is an infrastructure-only
  change: add a `postgres-auth` service (`POSTGRES_DB/USER/PASSWORD=auth`, no init script
  needed), point `DB_HOST` to it, and move data with `pg_dump`/`pg_restore`. No code change.
