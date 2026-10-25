-- Runs only when the Postgres volume is created for the first time.
-- Development-only credentials; not for any shared or production environment.
CREATE USER auth WITH PASSWORD 'auth';
CREATE DATABASE auth OWNER auth;
