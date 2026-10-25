# ADR 012 - Last write wins on task update

## Context
Update is a full replacement (`PUT /api/me/tasks/{id}`, spec 003). Two requests for the same task,
e.g. from two browser tabs of the same user, can overlap. Only the owner can write a task.

## Decision
No concurrency control: the last `PUT` to reach the database wins, and earlier concurrent changes are
lost. `updatedAt` shows when the stored version was written.

## Alternatives
- **Optimistic concurrency** (version column, `ETag` / `If-Match`, 412 on mismatch): prevents lost updates,
  but adds a version field, conditional headers and a new error path for every client.
- **Pessimistic locking**: holds row locks across requests; unfit for a stateless HTTP API.

## Consequences
- Concurrent edits of the same task by its owner can silently overwrite each other.
- Adding optimistic concurrency later is backward compatible if `If-Match` stays optional at first.
