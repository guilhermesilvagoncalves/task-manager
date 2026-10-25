# 005 - JWT required claims

## Goal
A token that is correctly signed but lacks the claims the task service depends on is rejected with 401,
never accepted forever and never turned into a 500.

## Context
The decoder (ADR 008) validates signature, `iss`, `exp`/`nbf` (when present) and `typ`. It does not require
`exp` or `sub` to exist. Today a signed token without `sub` passes validation and fails later in
`UUID.fromString(jwt.getSubject())` → 500; a signed token without `exp` would never expire.
Only the auth service can sign tokens, so this is defense in depth, not an open exploit.

## Rules
- `exp` is required.
- `sub` is required and must be a UUID (it is the task owner id).
- Only RS256 is accepted, declared explicitly (today it is the library default).
- Tokens issued by the auth service (spec 002 contract) keep working unchanged.

## Acceptance criteria
- [x] Signed token without `exp` → 401.
- [x] Signed token without `sub` → 401 (not 500).
- [x] Signed token with a blank `sub` → 401.
- [x] Signed token with a non-UUID `sub` (e.g. `"admin"`) → 401 (not 500).
- [x] All of the above return the same RFC 7807 401 body and `WWW-Authenticate: Bearer` as any invalid token;
      the body does not say which claim failed.
- [x] A token issued by the auth service is still accepted (existing tests stay green).
- [x] The accepted algorithm is declared in the decoder configuration, not left to the library default.

## Out of scope
- `aud` (audience) validation: the auth service does not emit `aud` yet; adding it needs a contract change in spec 002.
- Rate limiting JWKS refetches triggered by unknown `kid` values (listed as a known limitation).

## Tasks
- [x] 1. Decoder tests for missing `exp`, missing/blank/non-UUID `sub` → closes: first four criteria
- [x] 2. Required-claim validators + explicit RS256 in the decoder → closes: all criteria
- [x] 3. Slice test: missing-`sub` token through the endpoint returns the 401 problem body → closes: same 401 body
- [x] 4. ADR 008 updated with the required claims; README limitation list reviewed → closes: documentation
