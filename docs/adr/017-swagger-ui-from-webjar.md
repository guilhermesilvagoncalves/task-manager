# ADR 017 - Swagger UI from the webjar, without springdoc in the gateway

## Context
Spec 006 adds one Swagger UI, served by the gateway, over the two routed documents (`/api-docs/auth`,
`/api-docs/tasks`). The gateway has no endpoints of its own, so it must not publish an OpenAPI document.
springdoc 3.2.0 only enables its UI when its own document generation is on (`SwaggerConfig` is
`@ConditionalOnBean(SpringDocConfiguration)`, which needs `springdoc.api-docs.enabled=true`).

## Decision
The gateway serves a static page (`static/swagger-ui.html`) that loads the `org.webjars:swagger-ui` assets and lists
both documents. `webjars-locator-lite` (version managed by Boot) lets the page use version-less asset paths.
A gateway test fetches every asset the page references.

## Alternatives
- **springdoc UI, accepting the empty gateway document** at `/v3/api-docs`: no code, but publishes a document with
  no paths and breaks the spec criterion.
- **springdoc UI plus a filter that hides `/v3/api-docs`**: meets the criterion, but adds a class whose only job is
  to undo a library side effect, and keeps `/v3/api-docs/swagger-config` public.

## Consequences
- No Java code; the public surface is the page and its static assets.
- Upgrading Swagger UI is a `pom.xml` change only; a missing asset fails `mvn test`.
- UI options (e.g. persisted authorization) are set in the page, not in `application.yml`.
