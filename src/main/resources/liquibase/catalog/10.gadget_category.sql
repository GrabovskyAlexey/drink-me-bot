--liquibase formatted sql

--changeset drink-me-bot:gadget_category
-- Гаджеты и посуда (шейкер, джиггер, бокалы) - категория GADGET: их можно отметить в /ingredients, карточка коктейля
-- показывает, каких не хватает. Гаджеты необязательные (goods.is_essential FALSE) и в подбор не входят ни в каком режиме.
INSERT INTO drink_me_bot.goods_category (goods_id, category)
SELECT DISTINCT cg.goods_id, 'GADGET'
FROM drink_me_bot.cocktail_goods cg
JOIN drink_me_bot.goods g ON g.id = cg.goods_id AND NOT g.is_essential
WHERE cg.kind = 'gadget'
ON CONFLICT (goods_id) DO NOTHING;
