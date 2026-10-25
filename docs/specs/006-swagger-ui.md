# 006 - Swagger UI

## Goal
An evaluator can browse and try the whole API from a browser, through the gateway, without Postman or curl.
This replaces the spec 004 decision "Swagger UI is not added (YAGNI)": the JSON documents meet the requirement,
but are not usable for a live walkthrough.

## Rules
- One UI, served by the gateway, listing both documents (`/api-docs/auth` and `/api-docs/tasks`).
  No UI in the services: only the gateway is published (spec 001).
- "Try it out" calls go through the gateway (documents keep the relative server `/`).
- Protected task endpoints can be called by pasting the token from `POST /api/auth/login` into "Authorize".
- No new public surface beyond the UI page and its static assets; resource routes and their auth rules are unchanged.

## Acceptance criteria
- [x] `GET localhost:8080/swagger-ui.html` → 200 in the browser.
- [x] The UI offers both documents (auth and tasks) in its selector.
- [ ] From the UI: register → login → Authorize with the token → create, list, read, update, delete a task, all succeed.
- [ ] Calling a task endpoint from the UI without Authorize → 401 problem body.
- [x] The gateway does not publish an OpenAPI document of its own (it has no endpoints of its own).
- [x] Unknown paths still return the gateway's 404 problem body; the UI routes do not catch other paths.
- [x] README "API documentation" section points to the UI; spec 004 decision updated.
- [x] Dependency scan re-run after adding the UI dependency; findings handled as in spec 004.

## Assumptions to verify before implementing
- ~~springdoc's UI can run in the Spring Cloud Gateway (WebFlux) app with `springdoc.swagger-ui.urls` pointing to
  the routed documents, while the gateway's own document generation is disabled.~~
  **False** (springdoc 3.2.0): `SwaggerConfig` is `@ConditionalOnBean(SpringDocConfiguration)`, which needs
  `springdoc.api-docs.enabled=true`. With generation on, the gateway publishes an empty document at `/v3/api-docs`.
- The UI static assets are served by the gateway app itself and do not conflict with the gateway routes. **True.**
- With `webjars-locator-lite` on the classpath, Boot serves webjar assets at version-less paths
  (`/webjars/swagger-ui/swagger-ui-bundle.js`).

## Decisions
- No springdoc in the gateway. The UI is the `org.webjars:swagger-ui` webjar plus one static page
  (`/swagger-ui.html`) that lists both routed documents. No gateway document, no `swagger-config` endpoint.
- Asset paths in the page are version-less (`webjars-locator-lite`, version managed by Boot), so upgrading the
  webjar is a `pom.xml` change only. A gateway test fetches every asset the page references, so a broken
  page fails `mvn test`.

## Out of scope
- UI in each service, a custom UI theme, or exposing the UI outside local development.
- OAuth flows in the UI (the token is pasted manually).

## Tasks
- [x] 1. Spike: verify both assumptions; record the outcome here → closes: assumptions
- [x] 2. Add the UI to the gateway with both document URLs → closes: UI reachable, both documents, no gateway document
- [x] 3. Gateway test: UI page 200, unknown path still 404 problem → closes: routing criteria
- [ ] 4. Manual walkthrough register → CRUD from the UI; 401 without Authorize → closes: try-it-out criteria
- [x] 5. README + spec 004 decision + dependency scan → closes: documentation, scan
