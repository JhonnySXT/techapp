# Dockerfile для развертывания на Render.com
# Этот файл собирает сервер из исходников

# Используем образ с JDK для сборки (версия Gradle должна совпадать с проектом)
FROM gradle:8.13-jdk17-alpine AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем только файлы, необходимые для сборки
COPY gradle/ ./gradle/

# Создаем упрощенный settings.gradle.kts только для server модуля (без Android)
RUN cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "1.9.24"
        kotlin("plugin.serialization") version "1.9.24"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "Techapp"
include(":server")
EOF

# Явно создаем пустой корневой build.gradle.kts, чтобы Gradle не искал Android-плагины
RUN cat > build.gradle.kts << 'EOF'
// Пустой корневой build.gradle.kts для серверной сборки
// Все настройки находятся в server/build.gradle.kts
EOF

# Создаем gradle.properties без Windows-специфичных настроек
RUN cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.caching=false
org.gradle.configureondemand=false
EOF

# Копируем только server модуль
COPY server/build.gradle.kts ./server/
COPY server/src/ ./server/src/

# Копируем статические файлы фронтенда (для PWA)
COPY test-app.html ./
COPY config.js ./
COPY manifest.json ./
COPY sw.js ./
COPY icon-*.png ./

# Используем встроенный Gradle из образа
# Сначала проверяем структуру проекта
RUN echo "=== Project structure ===" && \
    ls -la && \
    echo "=== Server structure ===" && \
    ls -la server/ && \
    echo "=== Gradle version ===" && \
    gradle --version

# Собираем JAR файл с подробными логами
RUN gradle :server:shadowJar --no-daemon --stacktrace --info || \
    (echo "=== Build failed, showing full log ===" && \
     gradle :server:shadowJar --no-daemon --stacktrace --debug 2>&1 | tail -100 && \
     exit 1)

# Финальный образ с JRE
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR из стадии сборки
COPY --from=build /app/server/build/libs/server-all.jar app.jar

# Копируем статические файлы фронтенда из стадии сборки
COPY --from=build /app/test-app.html ./
COPY --from=build /app/config.js ./
COPY --from=build /app/manifest.json ./
COPY --from=build /app/sw.js ./
COPY --from=build /app/icon-*.png ./

# Создаем директорию для данных (база данных SQLite)
RUN mkdir -p /app/data
RUN mkdir -p /app/uploads

# Открываем порт
EXPOSE 8081

# Переменные окружения (можно переопределить в Render)
ENV PORT=8081
ENV HOST=0.0.0.0

# Запускаем сервер
ENTRYPOINT ["java", "-jar", "app.jar"]

