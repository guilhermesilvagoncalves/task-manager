# ADR 005 - JWT signing key generated at startup

## Context
`auth-service` issues JWTs that other services validate on their own through
`/.well-known/jwks.json` (spec 002). Only `auth-service` may hold the private key.

## Decision
Sign with RS256. Generate an RSA 2048 key pair in memory at startup, with a random `kid`.
Sign and publish the JWKS with Spring Security's `spring-security-oauth2-jose` (Nimbus),
without the Spring Security web filter chain.

## Alternatives
- **Key loaded from a PEM file / secret**: survives restarts and allows several instances,
  but needs key provisioning and rotation. Not needed while a single instance handles the load.
- **ES256**: smaller keys and tokens; RS256 is the most widely supported default.
- **HS256 (shared secret)**: every validator would hold the signing secret. Rejected by the spec.

## Consequences
- No key management.
- A restart invalidates every issued token; users must log in again.
- Only one `auth-service` instance can run, since each instance would have its own key.
