--liquibase formatted sql

--changeset drink-me-bot:create_user_cocktail
-- Отметки пользователя на коктейлях: избранное и «уже готовил». Без FK на коктейль: справочник импортируется заново.
CREATE TABLE drink_me_bot.user_cocktail
(
    user_id     BIGINT                   NOT NULL REFERENCES drink_me_bot.users (id) ON DELETE CASCADE,
    cocktail_id INTEGER                  NOT NULL,
    favorite    BOOLEAN                  NOT NULL DEFAULT FALSE,
    cooked      BOOLEAN                  NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, cocktail_id)
);

CREATE INDEX user_cocktail_favorite_idx ON drink_me_bot.user_cocktail (user_id) WHERE favorite;
