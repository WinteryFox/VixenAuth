CREATE EXTENSION pgcrypto;

CREATE TABLE users
(
    snowflake TEXT PRIMARY KEY,
    email     TEXT NOT NULL,
    username  TEXT NOT NULL,
    hash      TEXT NOT NULL,
    avatar    TEXT
);

CREATE TABLE tokens
(
    snowflake TEXT REFERENCES users (snowflake),
    token     TEXT,
    PRIMARY KEY (snowflake, token)
)