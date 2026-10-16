--liquibase formatted sql

--changeset drink-me-bot:create_cocktail_photo
-- Фото коктейля (одно на все языки): Telegram file_id, который администратор прислал боту в /admin.
-- file_id привязан к боту, поэтому на тестовом и боевом ботах фото загружаются отдельно.
-- Без FK на коктейль: справочник импортируется заново.
CREATE TABLE drink_me_bot.cocktail_photo
(
    cocktail_id INTEGER                  PRIMARY KEY,
    file_id     TEXT                     NOT NULL,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
