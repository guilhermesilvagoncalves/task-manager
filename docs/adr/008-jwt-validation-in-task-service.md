# ADR 008 - JWT validation in task-service with cached JWKS

## Context
Task endpoints need the authenticated user (spec 003). Tokens are RS256 JWTs issued by
`auth-service`, whose public keys are published at `/.well-known/jwks.json` (spec 002, ADR 005).
The auth service must not be called on every request.

## Decision
`task-service` is an OAuth2 resource server (Spring Security):
- `NimbusReactiveJwtDecoder` with the JWKS URI (`AUTH_JWKS_URI`) and an issuer validator (`AUTH_ISSUER`).
- The JWKS is fetched on the first request and cached. It is fetched again only when a token has an
  unknown `kid` (e.g. after `auth-service` restarts with a new key).
- The owner of a task is the token's `sub`. 401 responses are RFC 7807 problem details.
- The gateway only routes; it does not validate tokens.

## Alternatives
- **Validate at the gateway** and forward the user id in a header: one place for auth, but
  `task-service` would trust a header that anyone inside the network can set.
- **Introspection call to auth-service per request**: simplest revocation, but couples every request
  to auth-service availability and latency. Rejected by spec 003.
- **Shared public key in config**: no JWKS fetch, but breaks on every auth-service restart (ADR 005).

## Consequences
- Each service that needs auth repeats a small security config.
- A revoked or logged-out token stays valid until it expires (no revocation in spec 002).
- If auth-service is down, already cached keys keep working; a new key cannot be fetched.
