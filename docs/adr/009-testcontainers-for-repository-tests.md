# ADR 009 - Testcontainers for repository tests

## Context
Spec 003 requires that a user never reads or changes another user's task, that column limits match
the API limits (in code points), and that pagination is stable. These rules live in SQL, which mocks
cannot verify.

## Decision
`task-service` repository tests run against Postgres in a container (Testcontainers 2, same
`postgres:18.6-alpine` image as docker compose), wired with `@ServiceConnection`. Flyway
migrations run as in production.

## Alternatives
- **Karate only**: no extra dependency, but SQL rules are tested only with the full system running,
  slower to run and harder to pinpoint.
- **In-memory database (H2 with R2DBC)**: no Docker, but a different SQL dialect and different
  `VARCHAR` length semantics than Postgres.

## Consequences
- `mvn test` needs a running Docker daemon.
- The first run pulls the Postgres image; the container starts once per test class.
