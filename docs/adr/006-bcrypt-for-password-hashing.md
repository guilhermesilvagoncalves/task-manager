# ADR 006 - BCrypt for password hashing

## Context
Passwords must never be stored in plain text, and hashing must not block the request thread.

## Decision
Use `BCryptPasswordEncoder` (cost 10) from `spring-security-crypto`, run on `boundedElastic`.
An unknown email is compared against a dummy hash so it costs the same as a wrong password.

## Alternatives
- **Argon2id**: current OWASP first choice, memory-hard. Needs Bouncy Castle and memory tuning.
- **PBKDF2**: FIPS friendly, but weaker against GPU attacks than BCrypt or Argon2.

## Consequences
- No extra dependency besides `spring-security-crypto`.
- BCrypt only reads the first 72 bytes, so passwords are capped at 72 UTF-8 bytes (spec 002).
