--liquibase formatted sql

--changeset drink-me-bot:create_schema
CREATE SCHEMA IF NOT EXISTS drink_me_bot;

--changeset drink-me-bot:create_users
CREATE TABLE drink_me_bot.users
(
    id             BIGINT PRIMARY KEY,
    first_name     VARCHAR,
    last_name      VARCHAR,
    username       VARCHAR,
    language       VARCHAR(16),
    last_action_at TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--changeset drink-me-bot:create_user_profile
CREATE TABLE drink_me_bot.user_profile
(
    user_id    BIGINT PRIMARY KEY REFERENCES drink_me_bot.users (id),
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    is_admin   BOOLEAN NOT NULL DEFAULT FALSE,
    settings   JSONB   NOT NULL DEFAULT '{}'::jsonb,
    locale     VARCHAR(2)
);

--changeset drink-me-bot:create_flow_state
CREATE TABLE drink_me_bot.flow_state
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    flow_key         VARCHAR(64)  NOT NULL,
    step_key         VARCHAR(64)  NOT NULL,
    payload          JSONB,
    message_bindings JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX flow_state_user_key_uindex
    ON drink_me_bot.flow_state (user_id, flow_key);
