CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);

-- Emails are stored normalized (trimmed, lowercase), so a plain unique index is case-insensitive.
CREATE UNIQUE INDEX users_email_key ON users (email);
