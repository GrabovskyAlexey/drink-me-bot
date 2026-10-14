--liquibase formatted sql

--changeset drink-me-bot:user_activity_day
-- Дни активности пользователей (по МСК): по таблице считаются активные за 1/7/30 дней и история аудитории для /admin.
CREATE TABLE drink_me_bot.user_activity_day
(
    user_id BIGINT NOT NULL REFERENCES drink_me_bot.users (id) ON DELETE CASCADE,
    day     DATE   NOT NULL,
    PRIMARY KEY (user_id, day)
);

CREATE INDEX user_activity_day_day_idx ON drink_me_bot.user_activity_day (day);

--changeset drink-me-bot:user_activity_day_backfill
-- Прошлая активность неизвестна: берём только последнее действие каждого пользователя (приближение).
INSERT INTO drink_me_bot.user_activity_day (user_id, day)
SELECT id, (last_action_at AT TIME ZONE 'Europe/Moscow')::date
FROM drink_me_bot.users
WHERE last_action_at IS NOT NULL
ON CONFLICT DO NOTHING;

--changeset drink-me-bot:cocktail_view
-- Сколько раз открывали карточку коктейля (для популярных коктейлей в /admin). Без FK: справочник импортируется заново.
CREATE TABLE drink_me_bot.cocktail_view
(
    cocktail_id INTEGER NOT NULL PRIMARY KEY,
    views       BIGINT  NOT NULL DEFAULT 0
);
