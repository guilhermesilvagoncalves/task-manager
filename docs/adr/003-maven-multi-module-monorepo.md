# ADR 003 - Maven multi-module monorepo

## Context
The system has several services (`api-gateway`, `task-service`, more to come) in one
repository. They need a build and test setup, and dependency versions must stay
compatible (CLAUDE.md: latest GA and compatible).

## Decision
Use one Maven reactor: a root `pom.xml` (packaging `pom`) that inherits from
`spring-boot-starter-parent` and lists each service as a module. Every module inherits
from the root. Only the gateway imports the Spring Cloud BOM.

## Alternatives
- **One independent Maven project per service, in the same repository**: each service
  can upgrade Spring Boot on its own, but builds, tests and version alignment are
  duplicated.
- **One repository per service**: strongest isolation and independent release cycles,
  but too much overhead for a single-team POC, and cross-service changes span repositories.

## Consequences
- A single `mvn test` at the root builds and tests every service.
- Every service uses the same Spring Boot version. Today the gateway's Spring Cloud
  release train (2025.1.x supports Boot <4.2) caps the Boot version for all modules,
  `task-service` included.
- Dockerfiles must copy every module's `pom.xml`, because the reactor needs them to resolve.
- Extracting a service later means giving it its own parent and versions.
