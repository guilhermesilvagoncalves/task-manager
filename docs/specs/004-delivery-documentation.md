# 004 - Delivery

## Goal
The repository can be evaluated by someone who has only Docker and the README:
it starts, it is tested, and every relevant decision is explained.

## Scope
No new features. Documentation, verification and packaging of specs 001–003.

## Acceptance criteria

### Fresh start (README is the source of truth)
- [x] On a clean clone, following only the README, `docker compose up --build` starts the full system
  with Docker as the only requirement.
- [x] Every command in the README was executed from scratch, in order, and works as written.
- [x] The README states the JDK/Maven versions needed to build and test outside Docker.
- [x] Ports published by compose are listed; only the gateway is published (spec 001).

### README content
- [x] How to run, how to test (unit, slice, integration, Karate) and how to call the API with a token.
- [x] Assumptions: every business decision taken in specs 001–003 (e.g. past due dates allowed,
  last write wins, page size 10, `/api/me` prefix), each with a one-line reason.
- [x] Known limitations and next steps, consistent with the "Out of scope" sections of the specs.
- [x] Links to the specs and ADRs.

### API documentation
- [x] OpenAPI of auth-service and task-service is reachable through the gateway; every endpoint documents success and
  error responses, including 401 for protected endpoints.
- [x] Response shapes in the OpenAPI match the "Response shapes" of the specs.
- [x] Postman collection covers register → login → full task CRUD, with the token captured
  automatically after login.

### Decisions
- [x] An ADR exists for every choice with real alternatives taken in specs 001–003
  (versions, token signing and distribution, password hashing, Flyway over JDBC, gateway role,
  `/api/me` prefix, concurrency policy).
- [x] `docs/adr/010-stack-versions.md` is re-checked on delivery day: versions still latest GA, date updated.

### Quality gates
- [x] `mvn test` and the Karate suite pass on a fresh environment.
- [x] Dependency scan (OSV-Scanner) runs with no resolution failures; every finding is fixed or
  listed as accepted with reason and expiry in `osv-scanner.toml`.
- [x] No secrets in the repository (keys, real passwords); dev credentials are clearly marked as such.
- [x] Each spec's acceptance criteria are all checked `[x]`, or the unchecked ones are listed in
  the README as known limitations.

### Repository
- [ ] Public GitHub repository; history is small, reviewable commits (one per task).
- [x] No build output, IDE files or local artifacts committed.

## Decisions
- The OpenAPI documents are routed by the gateway (this replaces the spec 003 decision that they are
  not routed): `GET /api-docs/auth` → auth-service `/v3/api-docs`, `GET /api-docs/tasks` → task-service
  `/v3/api-docs`. They are outside `/api` so they never collide with resource paths. No token needed.
- The OpenAPI `servers` entry is relative (`/`): paths resolve against wherever the document was fetched
  from, so they work as written through the gateway. No forwarded headers are trusted.
- `/.well-known/jwks.json` is hidden from the auth-service document: it is internal and not routed by the gateway.
- Error responses reference a `Problem` schema (type, title, status, detail, instance, errors).
- Swagger UI is not added: the JSON documents are enough to evaluate the API (YAGNI).
- Dependency scan = `osv-scanner scan source -r .` plus `osv-scanner scan image` of the three service images.
  The source scan alone cannot resolve `api-gateway`'s transitive dependencies (Spring Cloud BOM) and reports
  nothing for it; the image scan reads the jars Maven actually resolved, so it has no resolution gaps.
- Vulnerable transitive versions are pinned in `dependencyManagement` (scram 3.4, ognl 3.4.17); runtime images run
  `apk upgrade` so Alpine fixes (brotli) apply without waiting for a new base image.
- Spec 001 criteria about `/api/ping` and the `skeleton` table are marked as superseded by spec 003.

## Out of scope
- New features, refresh tokens, key rotation, mTLS, rate limiting.
- Production deployment.

## Tasks
- [x] 1. Fresh-clone run of the README; fix every step that fails → closes: Fresh start
- [x] 2. README content (assumptions, limitations, links) → closes: README content
- [x] 3. OpenAPI review against Response shapes → closes: API documentation (OpenAPI)
- [x] 4. Postman collection → closes: API documentation (Postman)
- [x] 5. ADR completeness + versions re-check → closes: Decisions
- [x] 6. Dependency scan + accepted risks → closes: Quality gates (scan)
- [ ] 7. Final checklist across specs 001–003 → closes: Quality gates (criteria), Repository