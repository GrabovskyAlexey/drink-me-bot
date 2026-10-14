--liquibase formatted sql

--changeset drink-me-bot:create_goods_category
-- Категории обязательных товаров для выбора ингредиентов в боте. Категории не зависят от языка: подписи
-- берутся из messages_*.properties (ключи ingredients.category.<код в нижнем регистре>).
CREATE TABLE drink_me_bot.goods_category
(
    goods_id INTEGER PRIMARY KEY REFERENCES drink_me_bot.goods (id) ON DELETE CASCADE,
    category TEXT NOT NULL
);

CREATE INDEX goods_category_by_category ON drink_me_bot.goods_category (category);

INSERT INTO drink_me_bot.goods_category (goods_id, category)
SELECT g.id, c.category
FROM (VALUES
    -- Водка
    (1, 'VODKA'), (387, 'VODKA'), (661, 'VODKA'), (663, 'VODKA'),
    -- Виски и бурбон
    (37, 'WHISKY'), (62, 'WHISKY'), (125, 'WHISKY'), (132, 'WHISKY'), (287, 'WHISKY'), (662, 'WHISKY'), (1035, 'WHISKY'),
    -- Ром и кашаса
    (39, 'RUM'), (86, 'RUM'), (88, 'RUM'), (89, 'RUM'), (101, 'RUM'), (365, 'RUM'), (389, 'RUM'),
    -- Джин и женевер
    (799, 'GIN'), (244, 'GIN'),
    -- Текила и мескаль
    (93, 'TEQUILA'), (381, 'TEQUILA'), (143, 'TEQUILA'),
    -- Коньяк, кальвадос, писко
    (40, 'BRANDY'), (92, 'BRANDY'), (98, 'BRANDY'),
    -- Ликёры
    (46, 'LIQUEUR'), (54, 'LIQUEUR'), (55, 'LIQUEUR'), (60, 'LIQUEUR'), (61, 'LIQUEUR'), (100, 'LIQUEUR'),
    (104, 'LIQUEUR'), (111, 'LIQUEUR'), (116, 'LIQUEUR'), (124, 'LIQUEUR'), (130, 'LIQUEUR'), (284, 'LIQUEUR'),
    (380, 'LIQUEUR'), (394, 'LIQUEUR'), (395, 'LIQUEUR'), (396, 'LIQUEUR'), (397, 'LIQUEUR'), (398, 'LIQUEUR'),
    (399, 'LIQUEUR'), (402, 'LIQUEUR'), (405, 'LIQUEUR'), (406, 'LIQUEUR'), (407, 'LIQUEUR'), (408, 'LIQUEUR'),
    (409, 'LIQUEUR'), (410, 'LIQUEUR'), (411, 'LIQUEUR'), (412, 'LIQUEUR'), (414, 'LIQUEUR'), (415, 'LIQUEUR'),
    (423, 'LIQUEUR'), (427, 'LIQUEUR'), (665, 'LIQUEUR'), (666, 'LIQUEUR'),
    -- Вермуты, вино, херес
    (35, 'WINE'), (36, 'WINE'), (318, 'WINE'), (430, 'WINE'), (432, 'WINE'), (433, 'WINE'), (258, 'WINE'),
    (710, 'WINE'), (82, 'WINE'), (288, 'WINE'), (332, 'WINE'), (435, 'WINE'),
    -- Биттеры и амаро
    (47, 'BITTER'), (63, 'BITTER'), (85, 'BITTER'), (114, 'BITTER'), (303, 'BITTER'), (166, 'BITTER'), (358, 'BITTER'),
    -- Прочее: абсент, пиво, саке
    (113, 'OTHER'), (437, 'OTHER'), (565, 'OTHER')
) AS c (goods_id, category)
JOIN drink_me_bot.goods g ON g.id = c.goods_id;
