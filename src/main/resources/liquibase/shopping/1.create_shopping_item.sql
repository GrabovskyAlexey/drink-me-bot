--liquibase formatted sql

--changeset drink-me-bot:create_shopping_item
-- Список покупок: товары, которые пользователь собирается купить, и отметка «куплено».
CREATE TABLE drink_me_bot.shopping_item
(
    user_id  BIGINT                   NOT NULL REFERENCES drink_me_bot.users (id) ON DELETE CASCADE,
    goods_id INTEGER                  NOT NULL REFERENCES drink_me_bot.goods (id) ON DELETE CASCADE,
    bought   BOOLEAN                  NOT NULL DEFAULT FALSE,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, goods_id)
);
