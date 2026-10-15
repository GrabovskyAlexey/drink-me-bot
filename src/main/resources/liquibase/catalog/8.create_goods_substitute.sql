--liquibase formatted sql

--changeset drink-me-bot:create_goods_substitute
-- Замены товаров. Строка (goods_id, substitute_id) значит: если у пользователя есть substitute_id, то товар goods_id
-- в коктейле считается имеющимся (в карточке подписывается «заменено: ...»). Замены направленные: чтобы заменять в обе
-- стороны, нужны две строки. Новые пары добавляются отдельной миграцией.
CREATE TABLE drink_me_bot.goods_substitute
(
    goods_id      INTEGER NOT NULL REFERENCES drink_me_bot.goods (id) ON DELETE CASCADE,
    substitute_id INTEGER NOT NULL REFERENCES drink_me_bot.goods (id) ON DELETE CASCADE,
    PRIMARY KEY (goods_id, substitute_id),
    CHECK (goods_id <> substitute_id)
);

CREATE INDEX goods_substitute_by_substitute ON drink_me_bot.goods_substitute (substitute_id);

-- 130 Ликер Драй Оранж <-> 54 Трипл сек: по сути один и тот же апельсиновый ликёр
INSERT INTO drink_me_bot.goods_substitute (goods_id, substitute_id)
SELECT g1.id, g2.id
FROM drink_me_bot.goods g1
         JOIN drink_me_bot.goods g2 ON (g1.id, g2.id) IN ((130, 54), (54, 130));
