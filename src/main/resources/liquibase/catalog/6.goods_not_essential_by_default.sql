--liquibase formatted sql

--changeset drink-me-bot:goods_not_essential_by_default
-- Новые товары справочника (после обновления данных) по умолчанию необязательны: обязательным товар становится
-- только отдельной миграцией вместе с категорией. Иначе новые безалкогольные товары ломали бы подбор.
-- Уже проставленные значения не меняются.
ALTER TABLE drink_me_bot.goods ALTER COLUMN is_essential SET DEFAULT FALSE;
