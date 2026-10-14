# drink-me-bot

Telegram-бот на Kotlin / Spring Boot. Работает **только в личных чатах**: команды и сообщения из групп игнорируются, а при добавлении в группу или канал бот сразу выходит.

Каркас взят из `poi-bot` без доменной части, групповой логики, CI и деплоя.

## Запуск

```bash
cp .env.example .env              # заполните TELEGRAM_BOT_TOKEN и TELEGRAM_BOT_NAME
docker compose up -d postgres     # локальная БД
./gradlew bootRun                 # нужны переменные окружения из .env
./gradlew test                    # тесты
```

В @BotFather отключите добавление в группы: `/setjoingroups` → Disable.
