# ADR 004 - Multi-stage Docker build

## Context
Each service has its own Dockerfile (CLAUDE.md), and `docker compose up --build` must
start the whole system from scratch.

## Decision
Build each service inside Docker with a multi-stage Dockerfile:
- Build stage (`maven:<version>-eclipse-temurin-25-alpine`): copy the POMs, run
  `dependency:go-offline`, then copy the sources and run `package -DskipTests`.
- Runtime stage (`eclipse-temurin:25-jre-alpine`): only the JRE and the jar.

The build context is the repository root, because the reactor needs every module's POM (ADR 003).

## Alternatives
- **Build the jar on the host, and have the Dockerfile only copy it**: faster builds that
  reuse the local Maven cache, but the host needs Java 25 and Maven, and running
  `docker compose up --build` alone can ship a stale jar.
- **Cloud Native Buildpacks (`spring-boot:build-image`)**: no Dockerfile and good image
  defaults, but it conflicts with the one-Dockerfile-per-service rule and is less transparent.

## Consequences
- Docker is the only requirement for running the system; builds are reproducible.
- The first build is slow because dependencies are downloaded inside the image. Later
  builds reuse the dependency layer until a POM changes.
- The runtime image contains no JDK, Maven, or sources.
- Tests are skipped in the image build; `mvn test` runs them separately.
