# ADR 011 - `/api/me` prefix for user-scoped resources

## Context
Every task belongs to exactly one user, and a user only ever sees their own tasks (spec 003).
The owner always comes from the token, never from the request.

## Decision
Task endpoints live under `/api/me/tasks`. `me` is the authenticated caller; the path states that the
resource is scoped to them. There is no `/api/tasks` and no user id in the path.
Another user's task id returns 404, as if it did not exist.

## Alternatives
- **`/api/tasks`** filtered by the token: shorter, but the path hides that results depend on the caller
  and leaves room for a future admin "all tasks" endpoint to be confused with it.
- **`/api/users/{userId}/tasks`**: explicit, but the id in the path must be checked against the token
  on every call, and a mismatch invites 403 vs 404 leaks.

## Consequences
- The gateway routes only `/api/me/tasks/**` to task-service; any other path is a 404.
- An admin or shared view would need new paths (e.g. `/api/admin/tasks`), not a change to these.
