## Principles

Follow strictly YAGNI and KISS, in code and in communication.

- **Code**: implement only what the current problem requires. Choose the simplest solution that works. No abstraction, configuration, or generalization "for the future".
- **TDD**: first write the test and see it fail. Then write the minimum implementation that makes it pass.
- **Unclear rules**: if a business rule or requirement is ambiguous, stop and ask. Never assume.
- **Assumptions**: state every technical assumption explicitly and verify it (versions exist, dependency tree, logs) before relying on it.
- **Communication**: short and direct answers. Flag risks and trade-offs briefly.

## Workflow

- Specs live in `docs/specs/NNN-name.md`. Implement only what the current spec asks; update the spec before changing behavior.
- Never commit without my review. One testable change per commit.

## Stack

- Java 25, Spring Boot, Spring WebFlux.
- Dependencies: latest stable (GA) version with active support; never RC/milestone.
- Reactive: no blocking calls on the request path; blocking work only at startup or on `boundedElastic`.
- Docker: one Dockerfile per service and a `docker-compose.yml` that runs the full system.

## Design

- Low coupling, high cohesion, SOLID.
- Layered (controller → service → repository) where there is business logic.
- No interface with a single implementation unless a test or boundary needs it.
- All code and comments in English.
- Avoid creating more than 1 class per file, to keep responsibility separated
- Always check for dependencies on pom files, and remove a dependency once its not needed

## Testing

- **Unit**: business logic and critical components, mocked dependencies. TDD applies here.
- **Slice** (`@WebFluxTest`): every endpoint, success and failure statuses (400/401/404/405/409).
- **Karate**: full flow through the gateway against docker compose, when closing a feature.
- **Postman**: usage collection, updated when closing a feature; create it if it does not exist.
- Commands: `mvn test` | `docker compose up -d --build && mvn -pl e2e-tests test -DskipE2E=false`

## Documentation

- All documents live in `docs/`, except the README.
- Always update the README with running instructions in a human-readable format.
- API documentation: OpenAPI via springdoc.
- Decisions in `docs/adr/NNN-title.md` (context, decision, alternatives, consequences). Only for choices with real alternatives.