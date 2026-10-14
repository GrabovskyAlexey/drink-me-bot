--liquibase formatted sql

--changeset drink-me-bot:create_catalog_tables
-- Схема PostgreSQL для данных из result/final.
-- Принципы:
--   * id коктейлей и товаров сквозные между локалями (берутся с сайта);
--   * всё, что зависит от языка (названия, описания, теги, шаги, состав с количествами и единицами), хранится по локалям;
--   * количества и единицы лежат "как на сайте" в каждой локали (us - oz, fr - cl), без пересчёта;
--   * подбор по наличию использует только ингредиенты и флаг goods.is_essential, который проставляется вручную.

CREATE TABLE drink_me_bot.lang (
    code text PRIMARY KEY            -- ru, uk, de, es, us, fr, ua, it
);

-- Единицы измерения: код не зависит от языка, подпись - по локалям.
CREATE TABLE drink_me_bot.unit (
    code text PRIMARY KEY            -- ML, CL, L, OZ, G, KG, PIECE
);

CREATE TABLE drink_me_bot.unit_i18n (
    unit_code text NOT NULL REFERENCES drink_me_bot.unit (code),
    lang      text NOT NULL REFERENCES drink_me_bot.lang (code),
    label     text NOT NULL,         -- 'мл', 'oz', 'stück' ...
    PRIMARY KEY (unit_code, lang)
);

-- Товары: и ингредиенты, и гаджеты (тип задаёт cocktail_goods.kind, т.к. товар может быть и тем и другим).
CREATE TABLE drink_me_bot.goods (
    id           integer PRIMARY KEY,
    -- обязательность для подбора коктейлей; расставляется вручную (апельсиновый сок = FALSE и т.п.)
    is_essential boolean NOT NULL DEFAULT TRUE
);

CREATE TABLE drink_me_bot.goods_i18n (
    goods_id    integer NOT NULL REFERENCES drink_me_bot.goods (id),
    lang        text    NOT NULL REFERENCES drink_me_bot.lang (code),
    name        text    NOT NULL,
    description text    NOT NULL,
    PRIMARY KEY (goods_id, lang)
);

-- Теги зависят от локали и между языками не связываются; position - порядок показа.
CREATE TABLE drink_me_bot.goods_tag (
    goods_id integer NOT NULL,
    lang     text    NOT NULL,
    position integer NOT NULL,
    tag      text    NOT NULL,
    PRIMARY KEY (goods_id, lang, position),
    FOREIGN KEY (goods_id, lang) REFERENCES drink_me_bot.goods_i18n (goods_id, lang) ON DELETE CASCADE
);

CREATE TABLE drink_me_bot.cocktail (
    id integer PRIMARY KEY
);

-- Строка есть только для тех локалей, где коктейль существует на сайте.
CREATE TABLE drink_me_bot.cocktail_i18n (
    cocktail_id integer NOT NULL REFERENCES drink_me_bot.cocktail (id),
    lang        text    NOT NULL REFERENCES drink_me_bot.lang (code),
    name        text    NOT NULL,
    author      text,
    PRIMARY KEY (cocktail_id, lang)
);

CREATE TABLE drink_me_bot.cocktail_tag (
    cocktail_id integer NOT NULL,
    lang        text    NOT NULL,
    position    integer NOT NULL,
    tag         text    NOT NULL,
    PRIMARY KEY (cocktail_id, lang, position),
    FOREIGN KEY (cocktail_id, lang) REFERENCES drink_me_bot.cocktail_i18n (cocktail_id, lang) ON DELETE CASCADE
);

-- Шаги приготовления; step_no задаёт порядок показа (шаги могут отличаться между локалями).
CREATE TABLE drink_me_bot.cocktail_step (
    cocktail_id integer NOT NULL,
    lang        text    NOT NULL,
    step_no     integer NOT NULL,
    text        text    NOT NULL,
    PRIMARY KEY (cocktail_id, lang, step_no),
    FOREIGN KEY (cocktail_id, lang) REFERENCES drink_me_bot.cocktail_i18n (cocktail_id, lang) ON DELETE CASCADE
);

-- Состав коктейля в конкретной локали: количество и единица как на сайте.
CREATE TABLE drink_me_bot.cocktail_goods (
    cocktail_id integer    NOT NULL,
    lang        text       NOT NULL,
    kind        text       NOT NULL CHECK (kind IN ('ingredient', 'gadget')),
    goods_id    integer    NOT NULL,
    position    integer    NOT NULL,   -- порядок показа внутри kind
    name        text       NOT NULL,   -- название в составе (с брендом: 'Водка Царская'), может отличаться от goods_i18n.name
    amount      numeric    NOT NULL,
    unit_code   text       NOT NULL REFERENCES drink_me_bot.unit (code),
    PRIMARY KEY (cocktail_id, lang, kind, goods_id),
    FOREIGN KEY (cocktail_id, lang) REFERENCES drink_me_bot.cocktail_i18n (cocktail_id, lang) ON DELETE CASCADE,
    FOREIGN KEY (goods_id, lang) REFERENCES drink_me_bot.goods_i18n (goods_id, lang)
);

CREATE INDEX cocktail_goods_by_goods ON drink_me_bot.cocktail_goods (lang, kind, goods_id);

-- Запрос подбора коктейлей по наличию (с допуском на недостающие ингредиенты): db/suggest.sql
