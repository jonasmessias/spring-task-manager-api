-- ============================================================
-- V1 — Baseline migration (matches existing schema)
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    name        VARCHAR(255),
    username    VARCHAR(255) NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider    VARCHAR(255) NOT NULL DEFAULT 'local',
    avatar_url  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    token           VARCHAR(255),
    email           VARCHAR(255),
    expiration_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    token           VARCHAR(255),
    email           VARCHAR(255),
    expiration_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token           VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL REFERENCES users(id),
    expiration_date TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    ip_address      VARCHAR(255),
    user_agent      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    action      VARCHAR(30)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255),
    details     VARCHAR(500),
    timestamp   TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS workspaces (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    cover_url   VARCHAR(255),
    owner_id    VARCHAR(255) NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_members (
    id            VARCHAR(255) NOT NULL PRIMARY KEY,
    workspace_id  VARCHAR(255) NOT NULL REFERENCES workspaces(id),
    user_id       VARCHAR(255) NOT NULL REFERENCES users(id),
    role          VARCHAR(255) NOT NULL,
    joined_at     TIMESTAMP    NOT NULL,
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE IF NOT EXISTS boards (
    id            VARCHAR(255) NOT NULL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    cover_url     VARCHAR(255),
    owner_id      VARCHAR(255) NOT NULL REFERENCES users(id),
    workspace_id  VARCHAR(255) NOT NULL REFERENCES workspaces(id),
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board_members (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    board_id    VARCHAR(255) NOT NULL REFERENCES boards(id),
    user_id     VARCHAR(255) NOT NULL REFERENCES users(id),
    role        VARCHAR(255) NOT NULL,
    joined_at   TIMESTAMP    NOT NULL,
    UNIQUE (board_id, user_id)
);

CREATE TABLE IF NOT EXISTS board_lists (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    list_position   INTEGER,
    board_id        VARCHAR(255) NOT NULL REFERENCES boards(id),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cards (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(1000),
    status          VARCHAR(255) NOT NULL,
    card_position   INTEGER,
    list_id         VARCHAR(255) NOT NULL REFERENCES board_lists(id),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attachments (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(255) NOT NULL,
    file_key        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    file_size       BIGINT       NOT NULL,
    card_id         VARCHAR(255) NOT NULL REFERENCES cards(id),
    uploaded_by     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL
);
