# task-manager

Task manager built with Java 25, Spring Boot and Spring WebFlux.

## Modules
- `api-gateway`: Spring Cloud Gateway, the only entry point (port 8080). Routes `/api/**` to `task-service`.
- `task-service`: reactive service backed by PostgreSQL (R2DBC at runtime, Flyway migrations at startup).

## Requirements
- Docker with Docker Compose
- To build or test locally: Java 25 and Maven 3.9+

## Run the full system
```sh
docker compose up -d --build
curl localhost:8080/api/ping
# {"status":"ok","db":"PostgreSQL 18.6 ..."}
```

Only the gateway is published on the host. Stop everything and delete the database volume:
```sh
docker compose down -v
```

## Run the tests
```sh
mvn test
```

## Docs
- Specs: `docs/specs/`
- Architecture decisions: `docs/adr/`
