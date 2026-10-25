# ADR 013 - Registering an existing email returns 201

## Context
Email is the login identifier and is unique (spec 002). A register endpoint that answers differently for a
taken email lets anyone check which emails have an account (account enumeration), even though login
already hides this (same 401 for wrong password and unknown email).

## Decision
`POST /api/auth/register` with an email that already exists (any letter case, sequential or concurrent)
returns 201 with a fresh random `id` and `createdAt`; nothing is stored. The password is always hashed
before the insert (`INSERT ... ON CONFLICT DO NOTHING`), so both cases take about the same time.

## Alternatives
- **409 Conflict**: the usual REST answer and clearer for the user, but confirms that the email has an account.
- **202 Accepted plus an email to the address** ("you already have an account"): hides existence and
  still informs the owner, but needs email delivery, which is out of scope.

## Consequences
- A user who registers twice is not told they already have an account; their original password still works.
- The returned `id` of a duplicate registration is not a real user id; clients must not rely on it.
- Enumeration through register and login is closed; rate limiting is still missing (out of scope).
