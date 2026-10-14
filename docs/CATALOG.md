# Обновление справочника коктейлей

Справочник (коктейли, товары, составы, шаги) лежит в `src/main/resources/liquibase/catalog/`. Данные импортируются из `2.import_catalog.sql` (changeset с `runOnChange:true`), ручная разметка живёт в отдельных миграциях.

## Что хранится вручную
- `goods.is_essential` — обязательный ли товар для подбора (сейчас только алкоголь и биттеры).
- `goods_category` — категория товара для экрана `/ingredients`. Без категории обязательный товар выбрать нельзя.

Импорт эти данные не трогает: он пересоздаёт всё, кроме `goods`, а удаляет только товары, которых больше нет в данных.

## Как обновить данные
1. Положить новый сгенерированный файл данных на место `2.import_catalog.sql` (первая строка с `--changeset ... runOnChange:true` сохраняется). Изменение контрольной суммы заставит Liquibase выполнить его заново при следующем запуске.
2. Новые товары получают `is_essential = FALSE` (значение по умолчанию задано миграцией `6.goods_not_essential_by_default.sql`), поэтому подбор после обновления не меняется.
3. Если среди новых товаров есть обязательные (алкоголь, биттеры), написать **новую** миграцию `N.<название>.sql` в `catalog/`, которая ставит `is_essential = TRUE` и добавляет строки в `goods_category` (категории: `VODKA`, `WHISKY`, `RUM`, `GIN`, `TEQUILA`, `BRANDY`, `LIQUEUR`, `WINE`, `BITTER`, `OTHER`). Применённые миграции не редактируем. Строку `<include>` добавить в `catalog/changelog.xml`.
4. Проверить на локальной БД: `./gradlew build`, затем запуск и просмотр `/ingredients` и `/cocktails`. Импорт занимает ~10–20 секунд.
5. Список новых товаров видно запросом: `SELECT g.id, gi.name FROM drink_me_bot.goods g JOIN drink_me_bot.goods_i18n gi ON gi.goods_id = g.id AND gi.lang = 'ru' WHERE NOT EXISTS (SELECT 1 FROM drink_me_bot.goods_category c WHERE c.goods_id = g.id) ORDER BY gi.name;` (товары вне категорий — необязательные или ещё не размеченные).

## Обязательность товара по обратной связи
Менять `is_essential` у существующего товара можно только новой миграцией (`UPDATE drink_me_bot.goods SET is_essential = ... WHERE id IN (...)`, при необходимости строка в `goods_category`).
