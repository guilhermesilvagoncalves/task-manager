-- Runs only when the Postgres volume is created for the first time.
CREATE USER auth WITH PASSWORD 'auth';
CREATE DATABASE auth OWNER auth;
