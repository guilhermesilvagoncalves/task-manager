# task-manager

Task manager built with Java 25, Spring Boot and Spring WebFlux.

## Modules
- `api-gateway`: Spring Cloud Gateway, the only entry point (port 8080). Routes `/api/auth/**` to `auth-service`
  and `/api/me/tasks/**` to `task-service`. Any other path returns 404.
- `task-service`: tasks of the logged-in user, backed by PostgreSQL (R2DBC at runtime, Flyway migrations at startup).
  Validates tokens on its own with the auth service's public keys.
- `auth-service`: register and login, issues RS256 JWTs (1 hour). Own `auth` database on the same Postgres.
- `e2e-tests`: Karate tests through the gateway; they need the system running.

## Requirements
- Docker with Docker Compose
- To build or test locally: Java 25 and Maven 3.9+
- `mvn test` also needs Docker: task-service repository tests start a Postgres container (Testcontainers).

## Run the full system
```sh
docker compose up -d --build
```

Register and log in:
```sh
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Manage your tasks with the token (`TOKEN=eyJ...`). Every user sees only their own tasks:
```sh
# Create (status is optional, defaults to TODO)
curl -X POST localhost:8080/api/me/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Buy milk","description":"Two litres","dueDate":"2026-11-01"}'
# List, 10 per page, newest first
curl "localhost:8080/api/me/tasks?page=0" -H "Authorization: Bearer $TOKEN"
# Read, replace (all fields required) and delete
curl localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN"
curl -X PUT localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Buy milk","description":"Two litres","status":"DONE","dueDate":"2026-11-01"}'
curl -X DELETE localhost:8080/api/me/tasks/<id> -H "Authorization: Bearer $TOKEN"
```
Errors are RFC 7807 problem details (`application/problem+json`).

Other services validate tokens with the public keys at `http://auth-service:8082/.well-known/jwks.json`
(inside the docker network only). Tokens become invalid when `auth-service` restarts.
OpenAPI docs (inside the docker network): `http://auth-service:8082/v3/api-docs` and
`http://task-service:8081/v3/api-docs`.

If you ran the system before the `auth` database existed, recreate the volume once with
`docker compose down -v`: the Postgres init script only runs on an empty volume.

Only the gateway is published on the host. Stop everything and delete the database volume:
```sh
docker compose down -v
```

## Run the tests
Unit and slice tests:
```sh
mvn test
```

End-to-end tests (Karate) through the gateway:
```sh
docker compose up -d --build
mvn -pl e2e-tests test -DskipE2E=false
```

A Postman collection is in `docs/postman/task-manager.postman_collection.json`.

## Docs
- Specs: `docs/specs/`
- Architecture decisions: `docs/adr/`
