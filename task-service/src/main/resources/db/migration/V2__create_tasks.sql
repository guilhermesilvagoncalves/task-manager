-- VARCHAR(n) counts characters (code points in a UTF-8 database), matching the API limits.
CREATE TABLE tasks (
    id          UUID PRIMARY KEY,
    owner_id    UUID         NOT NULL,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status      VARCHAR(11)  NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date    DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX tasks_owner_created_at_idx ON tasks (owner_id, created_at DESC, id DESC);
