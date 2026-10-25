# task-manager

Task manager built with Java 25, Spring Boot and Spring WebFlux. Users register, log in and manage their own tasks.

## Modules
- `api-gateway`: Spring Cloud Gateway, the only entry point (port 8080). Routes `/api/auth/**` to `auth-service`,
  `/api/me/tasks/**` to `task-service` and the OpenAPI documents under `/api-docs/`. Any other path returns 404.
- `task-service`: tasks of the logged-in user, backed by PostgreSQL (R2DBC at runtime, Flyway migrations at startup).
  Validates tokens on its own with the auth service's public keys.
- `auth-service`: register and login, issues RS256 JWTs (1 hour). Own `auth` database on the same Postgres.
- `e2e-tests`: Karate tests through the gateway; they need the system running.

## Requirements
- To run the system: Docker with Docker Compose. Nothing else.
- To build or test outside Docker: JDK 25 and Maven 3.9+ (tested with Temurin/Corretto 25.0.1 and Maven 3.9.12;
  the Docker build uses Maven 3.10.0).
- `mvn test` also needs Docker: task-service repository tests start a Postgres container (Testcontainers).

## Run the full system
```sh
docker compose up -d --build
```

Ports published on the host:

| Port | Service |
|---|---|
| 8080 | `api-gateway` |

Postgres (5432), `task-service` (8081) and `auth-service` (8082) are reachable only inside the docker network.

Stop everything and delete the database volume:
```sh
docker compose down -v
```

If you ran the system before the `auth` database existed, run `docker compose down -v` once:
the Postgres init script only runs on an empty volume.

## Call the API
Register, log in and keep the token in `TOKEN`:
```sh
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
```
The login response is `{"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}`.

Manage your tasks. Every user sees only their own tasks:
```sh
# Create (status is optional, defaults to TODO); the response contains the task id
curl -X POST localhost:8080/api/me/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Buy milk","description":"Two litres","dueDate":"2026-11-01"}'
# List, 10 per page, newest first
curl "localhost:8080/api/me/tasks?page=0" -H "Authorization: Bearer $TOKEN"
# Read, replace (all fields required) and delete; replace <id> with the id returned by create
curl localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN"
curl -X PUT localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Buy milk","description":"Two litres","status":"DONE","dueDate":"2026-11-01"}'
curl -X DELETE localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN"
```
Errors are RFC 7807 problem details (`application/problem+json`).

### API documentation
OpenAPI documents (no token needed):
```sh
curl localhost:8080/api-docs/auth
curl localhost:8080/api-docs/tasks
```

A Postman collection is in [`docs/postman/task-manager.postman_collection.json`](docs/postman/task-manager.postman_collection.json).
Run `Auth > Register`, then `Auth > Login` (stores the token), then the `Tasks` requests in order.

## Run the tests
Unit, slice and integration tests:
```sh
mvn test
```
- Unit: services and validators with mocked dependencies.
- Slice: every endpoint with `@WebFluxTest`, success and error statuses.
- Integration: task repository against a real Postgres (Testcontainers), gateway routes with `@SpringBootTest`.

End-to-end tests (Karate) through the gateway, with the system running:
```sh
docker compose up -d --build
mvn -pl e2e-tests test -DskipE2E=false
```

Dependency scan with [OSV-Scanner](https://google.github.io/osv-scanner/), after building the images:
```sh
osv-scanner scan source -r .
osv-scanner scan image task-manager-api-gateway:latest
osv-scanner scan image task-manager-auth-service:latest
osv-scanner scan image task-manager-task-service:latest
```
The image scan covers the jars that actually ship (resolved by Maven) and the Alpine packages; the source scan adds
test-only dependencies. Accepted findings, if any, go in `osv-scanner.toml` with a reason and an expiry date.

## Assumptions
Business decisions taken where the requirements left a choice:

- **Email is case-insensitive and stored trimmed and lowercase**: `A@x.com` and `a@x.com` are the same person.
- **Password 8 to 72 characters, at most 72 UTF-8 bytes**: BCrypt ignores bytes after 72.
- **Registering an existing email returns 201 and stores nothing**: the response never reveals that an email exists.
- **Wrong password and unknown email get the same 401**: same reason, no account enumeration.
- **Tokens last 1 hour, no refresh token**: enough for a session; refresh was out of scope.
- **Signing key generated in memory at startup; a restart of `auth-service` invalidates every token**:
  no key storage to manage ([ADR 005](docs/adr/005-jwt-signing-key-generated-at-startup.md)).
- **Task paths live under `/api/me/tasks`**: the path states that the resource belongs to the caller
  ([ADR 011](docs/adr/011-api-me-prefix.md)).
- **The owner comes only from the token; an `owner` field in the body is ignored**: a user cannot write to another user.
- **Another user's task returns 404, never 403**: the response does not reveal that the task exists.
- **Title (1–100) and description (1–500) are required, counted in Unicode code points and trimmed**:
  an emoji counts as one character, as a user would expect.
- **Status defaults to `TODO` and any transition is allowed**: no workflow was required.
- **Due date is required and may be in the past**: users can record overdue or already-done work.
- **Update is a full replacement (PUT); last write wins**: a task has one owner, so concurrent edits are rare
  ([ADR 012](docs/adr/012-last-write-wins.md)).
- **Page size fixed at 10, first page 0, newest first, ties broken by id**: stable paging without client options.

## Known limitations and next steps
- No refresh token, logout or token revocation; tokens die when `auth-service` restarts.
- No key rotation, mTLS, rate limiting or account lockout.
- No password reset, email verification, roles or admin views.
- No PATCH, status transition rules or optimistic concurrency (ETag / If-Match): concurrent updates overwrite each other.
- No filtering, search, client-defined page size or sort; tasks cannot be shared between users.
- Credentials in `docker-compose.yml` and `docker/postgres/` are for local development only.
- Not set up for production deployment.

## Docs
- Specs: [001 walking skeleton](docs/specs/001-walking-skeleton.md), [002 auth server](docs/specs/002-auth-server.md),
  [003 task service](docs/specs/003-task-service.md), [004 delivery](docs/specs/004-delivery-documentation.md)
- Architecture decisions:
  - [001 Spring Cloud Gateway](docs/adr/001-spring-cloud-gateway.md)
  - [002 Flyway for schema migrations](docs/adr/002-flyway-for-schema-migrations.md)
  - [003 Maven multi-module monorepo](docs/adr/003-maven-multi-module-monorepo.md)
  - [004 Multi-stage Docker build](docs/adr/004-multi-stage-docker-build.md)
  - [005 JWT signing key generated at startup](docs/adr/005-jwt-signing-key-generated-at-startup.md)
  - [006 BCrypt for password hashing](docs/adr/006-bcrypt-for-password-hashing.md)
  - [007 Database per service on a shared instance](docs/adr/007-database-per-service-on-shared-instance.md)
  - [008 JWT validation in task-service](docs/adr/008-jwt-validation-in-task-service.md)
  - [009 Testcontainers for repository tests](docs/adr/009-testcontainers-for-repository-tests.md)
  - [010 Stack versions](docs/adr/010-stack-versions.md)
  - [011 `/api/me` prefix](docs/adr/011-api-me-prefix.md)
  - [012 Last write wins](docs/adr/012-last-write-wins.md)
