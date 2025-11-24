# Dockerfile для развертывания на Render.com
# Этот файл собирает сервер из исходников

# Используем образ с JDK для сборки (версия Gradle должна совпадать с проектом)
FROM gradle:8.13-jdk17-alpine AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем только файлы, необходимые для сборки
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Копируем только server модуль
COPY server/build.gradle.kts ./server/
COPY server/src/ ./server/src/

# Собираем JAR файл
RUN ./gradlew :server:shadowJar --no-daemon

# Финальный образ с JRE
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR из стадии сборки
COPY --from=build /app/server/build/libs/server-all.jar app.jar

# Создаем директорию для данных (база данных SQLite)
RUN mkdir -p /app/data

# Открываем порт
EXPOSE 8081

# Переменные окружения (можно переопределить в Render)
ENV PORT=8081
ENV HOST=0.0.0.0

# Запускаем сервер
ENTRYPOINT ["java", "-jar", "app.jar"]

