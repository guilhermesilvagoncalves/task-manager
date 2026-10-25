# 002 - Authentication

## Goal
Users can register and log in. Login returns a token that other services can validate
on their own, without calling the auth service on every request.

## Business rules
- Email is the login identifier; it is unique and case-insensitive ("A@x.com" = "a@x.com").
- Email is stored normalized (trimmed, lowercase).
- Password: 8 to 72 characters and at most 72 UTF-8 bytes (BCrypt limit).
- A login failure never reveals whether the email exists: wrong password and unknown email
  get the same status, the same message and similar response time.
- Passwords are never stored or logged in plain text.
- auth server should use the infrastructure created before, using gateway and being exposed only for
  services inside the same docker network
- Registering an email that already exists (any letter case, sequential or concurrent) returns
  201 with a fresh random `id` and `createdAt`; nothing is stored. The response never reveals
  whether the email exists.

## Token contract
- Signed with an asymmetric key; only the auth service holds the private key.
- Public keys are published at `GET /.well-known/jwks.json` so other services can validate tokens.
- Claims: `iss` (auth service), `sub` (user id, immutable), `iat`, `exp`. No sensitive data in claims.
- Lifetime: 1 hour.

## Acceptance criteria

### Register — `POST /api/auth/register`
- [ ] Valid payload → 201 with `id`, `email`, `createdAt`; no password or hash in the response.
- [ ] Email already registered (any letter case) → 201, to avoid enumeration.
- [ ] Invalid email, or password outside 8–72 → 400 with the error per field.
- [ ] Malformed JSON or missing body → 400.
- [ ] Two simultaneous requests with the same email → both 201, exactly one user stored.
- [ ] Register with a password over the byte limit (e.g. "é"×72) → 400, never 500.
- [ ] Login with an over-limit password → 401, never 500.

### Login — `POST /api/auth/login`
- [ ] Valid credentials → 200 with `accessToken`, `tokenType: "Bearer"`, `expiresIn` (seconds).
- [ ] Wrong password → 401.
- [ ] Unknown email → 401 with the same body as wrong password.
- [ ] Email in a different letter case logs in normally.
- 

### Keys — `GET /.well-known/jwks.json`
- [ ] Returns the public key; the response contains no private key material.
- [ ] The `kid` in every token header matches a key in the JWKS.
- [ ] A token signed by another key, with a tampered payload, or with `alg` other than the
  agreed one is rejected by a validator using this JWKS.

### General
- [ ] Unsupported method on an auth route → 405; unknown route under `/api/auth` → 404.
- [ ] Error responses follow a single format (RFC 7807).
- [ ] Hashing does not block the request thread.

## Decisions
- Module `auth-service`, port 8082, not published on the host.
- Gateway routes only `/api/auth/**` to `auth-service`. JWKS is internal only:
  `http://auth-service:8082/.well-known/jwks.json`.
- Own database `auth` and user `auth` on the existing Postgres instance, created by a Postgres
  init script (runs only on a fresh volume: `docker compose down -v` once). Flyway in
  `auth-service` owns the `users` table (ADR 007).
- Signing: RS256, RSA 2048 key pair generated in memory at startup (ADR 005). A restart
  invalidates every issued token.
- `iss` = `http://auth-service:8082`.
- Password hashing: BCrypt on `boundedElastic` (ADR 006).

## Versions
Checked on 2026-10-24. Same base versions as spec 001, plus:

| Component | Version | Source |
|---|---|---|
| Spring Security (crypto, oauth2-jose) | 7.1.2 | Managed by Boot 4.1.2 |
| nimbus-jose-jwt | 10.9.1 | Transitive from spring-security-oauth2-jose |
| springdoc-openapi | 3.2.0 | Maven Central metadata (latest GA, built on Boot 4.1.2) |
| Karate (`karate-junit6`) | 2.1.4 | Maven Central metadata (latest GA) |

## Out of scope
- Refresh token, logout, token revocation.
- Password reset, email verification.
- Rate limiting and account lockout.
- Roles/permissions.
- Restricted password policy

## Tasks
- [x] 1. Users database creation migration on current postgres instance
- [x] 2. Users table migration + repository (unique email index)
- [x] 3. Register endpoint → closes: 201, duplicate 201, 400, concurrent register
- [ ] 4. Login with constant-time failure → closes: 200, 401 wrong password, 401 unknown email
- [ ] 5. Token issuing + JWKS → closes: kid matches, no private key, tampered token rejected
- [ ] 6. Unmatched routes → closes: 405, 404
- [ ] 7. Separate DTOs into new classes