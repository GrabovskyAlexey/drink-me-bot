# Repository Guidelines
- Всегда общайся и веди заметки на русском языке.
- Все файлы сохраняй в кодировке UTF-8 без BOM.
- Не выполняй коммиты без моего явного запроса; такие запросы считаю разовыми, а не постоянными.
- Не меняй текст и логику уже существующего функционала (включая команды) без прямого указания.

Каркас создан по образцу `poi-bot` (соседний каталог) без доменной части. **Бот работает только в личных чатах**: группы не поддерживаются.

## Структура
Код: `src/main/kotlin/ru/grabovsky/drinkmebot`: `bot` — приём апдейтов, `service` — бизнес-логика, `strategy` — команды и сценарии (flow), `entity`/`repository` — данные, `config`/`framework`/`util` — инфраструктура. Ресурсы: `application.yaml`, шаблоны Freemarker `message/template/{flow}/{step}{,_ru,_en}.ftl`, локализация `messages_*.properties`, миграции Liquibase: корневой кумулятивный `liquibase/changelog.xml` включает по одному `changelog.xml` на папку (`liquibase/mvp/`, далее по папке на задачу), сами миграции — formatted SQL `N.name.sql`. Схема БД — `drink_me_bot`.

## Команды
- `./gradlew build` — сборка и тесты; `./gradlew test`; `./gradlew jacocoTestReport`
- `./gradlew bootRun` — запуск (нужны `PG_URL`, `PG_USER`, `PG_PASSWORD`, `TELEGRAM_BOT_NAME`, `TELEGRAM_BOT_TOKEN`)
- `docker compose up -d postgres` — локальная БД

## Архитектура
- `bot/Bot.kt` наследует `CommandLongPollingTelegramBot`, регистрирует `AbstractCommand` и публикует их через `SetMyCommands`. `Bot.filter` отсекает команды вне личных чатов.
- Только личные чаты: `ReceiverServiceImpl` игнорирует сообщения и callback-и из групп, а при добавлении бота в группу/канал (`my_chat_member`) вызывает `LeaveChat`. В @BotFather отключено `/setjoingroups`.
- Команда (`AbstractCommand`) привязана к `FlowKey` (`FlowKeys.*`) и запускает сценарий через `FlowEngine.start`. Не-командные апдейты обрабатывает `ReceiverServiceImpl`: берёт последний активный flow из БД и делегирует в `FlowEngine`; callback без состояния перезапускает flow по payload (`FlowCallbackPayload`).
- `FlowEngine` держит `FlowHandler` по ключам, сохраняет шаг, payload (JSON) и биндинги сообщений в `flow_state`. Действия (`SendMessageAction`, `EditMessageAction`, ...) исполняет `TelegramFlowActionExecutor`: рендерит Freemarker-шаблон, строит клавиатуру, отслеживает messageId.
- Статичные экраны — `AbstractStaticFlow` (см. `strategy/flow/start`, `strategy/flow/help`). Интерактивные — свои `*FlowState`/`*Step` + `PromptSupport`.
- Неизвестные команды обрабатывает `UnknownCommandService`; автоудаление служебных сообщений — `ChatCleanupPolicy` (`UserSettings.cleanChat`).

## Доменная модель
- Слои: `entity` (JPA, `kotlin-jpa` + `allOpen`, схема в `@Table(schema = "drink_me_bot")`, JSON-поля через `@JdbcTypeCode(SqlTypes.JSON)`) → `repository` (Spring Data JPA) → `service/interfaces` + `service/*Impl` (один интерфейс — одна реализация) → `strategy/flow/<feature>`.
- Фича = пакет `strategy/flow/<feature>` (`*Flow`, `*FlowState`, `*Step : FlowStep`) + `FlowKeys.<FEATURE>` + запись в `Command` + `*Command` + шаблоны `message/template/<feature>/<step>{,_ru,_en}.ftl` + ключи в `messages_*.properties` (lower.snake.case).
- Пользователь: `User` + `UserProfile` (1:1, `@MapsId`; флаги `isBlocked`/`isAdmin`, `locale`, настройки — JSON `UserSettings`).
- Миграции Liquibase: кумулятивный `changelog.xml` → `<папка>/changelog.xml` → нумерованные SQL-файлы. Применённые миграции не редактируем.
- Тесты: Kotest `ShouldSpec` + MockK, имена на английском (`shouldDoSomethingWhenPrecondition`), БД — Testcontainers.

## Стиль и безопасность
Kotlin Style Guide, 4 пробела, один публичный класс на файл, `val` и неизменяемые коллекции. Секреты не коммитим: переменные окружения, локально — `.env` и `application-local.yaml` (в `.gitignore`). Коммиты — на английском, в повелительном наклонении.
- Шаблоны локализуются двумя файлами: `step.ftl` (ru, по умолчанию) и `step_en.ftl`. Тексты с пользовательскими данными — только `?html` и `FlowParseMode.HTML`.
- Тексты кнопок и алертов — в `messages_{ru,en}.properties` (ключи `buttons.*`, `alerts.*`).

## Ветки и деплой
- Процесс: фича-ветка → `dev` → `master`. Ветки фич называем `feature/<название>`, релизные подготовки — `release_X.Y`.
- `.github/workflows/ci.yml`: тесты на каждый pull request в `dev` и `master` и на push в `dev`.
- `.github/workflows/deploy.yml`: на каждый push в `master` — тесты → версия `vX.Y.Z` (major.minor из `version.txt`, patch — автоинкремент) → `docker build`/`push` в GHCR → `scripts/deploy-drinkmebot.sh` на сервере по SSH → git-тег и GitHub Release. Секреты GitHub: `VPS_SSH_HOST`, `VPS_SSH_PORT`, `SSH_USER`, `VPS_SSH_KEY`. Подробности — `docs/DEPLOY.md`.
- Версия приложения задаётся CI через `-PappVersion` (локально `0.0.1-SNAPSHOT`) и пишется в лог при старте (`StartupInfo`). Jar — `build/libs/drinkmebot.jar`, `Dockerfile` копирует именно её.
- Конфиг на сервере — `~/drinkmebot/.env` (образец `scripts/.env.server.example`), профиль `prod`, порт приложения по умолчанию 8092 на `127.0.0.1`.

## Ингредиенты и подбор
- Справочник коктейлей — Liquibase `liquibase/catalog/` (схема, импорт данных `runOnChange`, `is_essential`, `user_ingredient`, `goods_category`). Обязательность (`goods.is_essential`) меняем только новой миграцией. Читаем справочник через `NamedParameterJdbcTemplate` (таблицы с префиксом `drink_me_bot.`), пользовательские данные — через JPA (`UserIngredient`).
- Локаль данных (`DataLocale`): en→us, Telegram `uk`→`ua`, остальные как есть; неизвестные — `us`.
- `/ingredients` (`IngredientsFlow`): категории (`IngredientCategory`, таблица `goods_category`) → страница с ✅ → поиск. Callback-и несут вид (`V:<вид>:<стр>`, `T:<id>:<вид>:<стр>`), состояние хранит только поисковый запрос.
- Тексты: `messages.properties` — английский по умолчанию (для неподдерживаемых языков), `messages_ru`/`messages_en`; шаблоны `step.ftl` — диспетчер (ru → `step_ru.ftl`, иначе `step_en.ftl`).
- `/cocktails` (`CocktailsFlow`): разделы «можно / не хватает 1 / не хватает 2» (по 8 коктейлей на странице) → карточка (`CocktailCardService`), «что купить». Всё в одном перерисовываемом сообщении; состояния нет, callback-и: `M`, `S:<не хватает>:<стр>`, `C:<id>:<не хватает>:<стр>`, `B`. В карточке обязательные ингредиенты помечены ✅/❌, необязательные — •. Пагинация — `paginationButtons` (`flow/core/support`).
- Разметка сообщений — только HTML (`FlowParseMode.HTML` по умолчанию в `FlowMessage` и `buildMessage`): все подстановки данных в шаблонах через `?html`, форматирование `<b>`, `<i>`, `<code>`. Markdown не используем: его экранирование хрупко (`_`, `*`, `.`, `-` в названиях ломают сообщение).

