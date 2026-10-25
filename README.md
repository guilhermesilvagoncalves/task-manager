# task-manager

Task manager built with Java 25, Spring Boot and Spring WebFlux.

## Modules
- `api-gateway`: Spring Cloud Gateway, the only entry point (port 8080). Routes `/api/auth/**` to `auth-service`
  and every other `/api/**` to `task-service`.
- `task-service`: reactive service backed by PostgreSQL (R2DBC at runtime, Flyway migrations at startup).
- `auth-service`: register and login, issues RS256 JWTs (1 hour). Own `auth` database on the same Postgres.
- `e2e-tests`: Karate tests through the gateway; they need the system running.

## Requirements
- Docker with Docker Compose
- To build or test locally: Java 25 and Maven 3.9+

## Run the full system
```sh
docker compose up -d --build
curl localhost:8080/api/ping
# {"status":"ok","db":"PostgreSQL 18.6 ..."}
```

Register and log in:
```sh
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"john@example.com","password":"password123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Other services validate tokens with the public keys at `http://auth-service:8082/.well-known/jwks.json`
(inside the docker network only). Tokens become invalid when `auth-service` restarts.
OpenAPI docs: `http://auth-service:8082/v3/api-docs` (inside the docker network).

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
