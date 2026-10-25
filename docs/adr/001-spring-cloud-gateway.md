# ADR 001 - Spring Cloud Gateway as API gateway

## Context
All client traffic must enter through a single gateway that routes to backend services.
The stack is Java 25 / Spring Boot / WebFlux, and only the gateway is published on the host.

## Decision
Use Spring Cloud Gateway (reactive, `spring-cloud-starter-gateway-server-webflux`) as a
dedicated `api-gateway` module, with routes declared in `application.yml`.

## Alternatives
- **Nginx / Traefik**: lightweight and fast, but a second technology stack to configure and
  test; cross-cutting concerns (auth, resilience) would live outside the Java code.
- **No gateway** (publish services directly): simplest, but exposes every service and
  spreads cross-cutting concerns across services.

## Consequences
- Same stack, build, and test tooling as the services.
- Future auth and resilience can be implemented as gateway filters in Java.
- Extra JVM process and an extra network hop per request.
- Spring Cloud release trains lag Spring Boot; the Boot version is bounded by the latest
  Spring Cloud GA that supports it.
