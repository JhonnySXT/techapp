# Dockerfile для развертывания сервера Techapp
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR файл сервера
COPY server/build/libs/server-all.jar app.jar

# Создаем директорию для данных (база данных SQLite)
RUN mkdir -p /app/data

# Открываем порт (по умолчанию 8081, но можно изменить через переменную окружения)
EXPOSE 8081

# Переменные окружения
ENV PORT=8081
ENV HOST=0.0.0.0

# Запускаем сервер
ENTRYPOINT ["java", "-jar", "app.jar"]

