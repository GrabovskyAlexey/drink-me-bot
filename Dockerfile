# Образ собирается из готовой jar: сначала `./gradlew bootJar`, затем `docker build -t drinkmebot .`
FROM eclipse-temurin:21-jre

# Связывает пакет в GHCR с репозиторием: образ наследует его видимость (публичный репозиторий — публичный образ)
# и GITHUB_TOKEN из Actions получает право публиковать новые версии
LABEL org.opencontainers.image.source="https://github.com/GrabovskyAlexey/drink-me-bot"

RUN useradd --system --uid 10001 --create-home drinkmebot \
    && mkdir -p /opt/app/logs \
    && chown -R drinkmebot:drinkmebot /opt/app

WORKDIR /opt/app
COPY --chown=drinkmebot:drinkmebot build/libs/drinkmebot.jar app.jar

USER drinkmebot
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
