# ADR 014 - Offset pagination with fixed page size

## Context
The task list returns the caller's tasks newest first, 10 per page, with total count and total pages
(spec 003). Pagination must be stable: no task appears twice or is skipped across pages.

## Decision
Page number + fixed size (`?page=N`, `LIMIT 10 OFFSET N*10`), ordered by `created_at DESC, id DESC`.
The `id` breaks ties between tasks created in the same microsecond, so the order is total and repeatable.
The total is a separate `count(*)` per owner.

## Alternatives
- **Keyset (cursor) pagination** (`?after=<createdAt,id>`): stable even while tasks are created or deleted
  and fast on deep pages, but no "jump to page N" and no total pages, which the spec requires.
- **Client-defined page size and sort**: more flexible, explicitly out of scope.

## Consequences
- Stability holds for a list that does not change while it is read. A task created or deleted between two
  page requests shifts the offsets: a task can repeat on the next page or be skipped.
- Each list request runs two queries (page + count); fine for per-user task volumes.
- Moving to keyset later means a new query parameter and response field, not a breaking change, if `page` stays.
