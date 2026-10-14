--liquibase formatted sql

--changeset drink-me-bot:create_user_ingredient
-- Ингредиенты, которые есть у пользователя (выбор запоминается).
CREATE TABLE drink_me_bot.user_ingredient
(
    user_id    BIGINT                   NOT NULL REFERENCES drink_me_bot.users (id) ON DELETE CASCADE,
    goods_id   INTEGER                  NOT NULL REFERENCES drink_me_bot.goods (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, goods_id)
);

CREATE INDEX user_ingredient_by_goods ON drink_me_bot.user_ingredient (goods_id);
