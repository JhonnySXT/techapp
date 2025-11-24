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

# Создаем статические файлы фронтенда (для PWA) прямо в образе
# Это гарантирует, что файлы всегда будут доступны

# Создаем config.js (новый формат с APP_CONFIG)
RUN cat > config.js << 'EOF'
// Конфигурация приложения для dev/prod окружений
(function() {
    'use strict';
    
    // Автоматическое определение окружения
    const isProduction = window.location.protocol === 'https:' || 
                        window.location.hostname !== 'localhost' && 
                        window.location.hostname !== '127.0.0.1';
    
    // Базовый URL API
    const getApiBase = () => {
        if (isProduction) {
            const protocol = window.location.protocol;
            const hostname = window.location.hostname;
            return `${protocol}//${hostname}/api/v1`;
        } else {
            return 'http://localhost:8081/api/v1';
        }
    };
    
    // Базовый URL для файлов (фотографии)
    const getFileBase = () => {
        if (isProduction) {
            const protocol = window.location.protocol;
            const hostname = window.location.hostname;
            return `${protocol}//${hostname}`;
        } else {
            return 'http://localhost:8081';
        }
    };
    
    // Экспортируем конфигурацию
    window.APP_CONFIG = {
        API_BASE: getApiBase(),
        FILE_BASE: getFileBase(),
        IS_PRODUCTION: isProduction,
        WS_URL: isProduction 
            ? `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.hostname}/ws`
            : 'ws://localhost:8081/ws'
    };
    
    console.log('📱 Конфигурация приложения:', {
        environment: isProduction ? 'PRODUCTION' : 'DEVELOPMENT',
        apiBase: window.APP_CONFIG.API_BASE,
        fileBase: window.APP_CONFIG.FILE_BASE
    });
})();
EOF

# Создаем manifest.json
RUN cat > manifest.json << 'EOF'
{
  "name": "Загородный клуб 'Дача'",
  "short_name": "Дача",
  "description": "Приложение для управления заявками в загородном клубе 'Дача'",
  "start_url": "/test-app.html",
  "display": "standalone",
  "background_color": "#2d5016",
  "theme_color": "#2d5016",
  "icons": [
    {
      "src": "/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
EOF

# Создаем sw.js (Service Worker) - базовая версия
RUN cat > sw.js << 'EOF'
const CACHE_NAME = 'techapp-cache-v1';
self.addEventListener('install', (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});
self.addEventListener('fetch', (event) => {
    event.respondWith(fetch(event.request).catch(() => caches.match(event.request)));
});
EOF

# Копируем test-app.html и иконки
# Файл должен быть в репозитории
COPY test-app.html ./
# Иконки копируем (если есть в репозитории)
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
# Файлы config.js, manifest.json и sw.js уже созданы в стадии build через RUN cat
COPY --from=build /app/config.js ./
COPY --from=build /app/manifest.json ./
COPY --from=build /app/sw.js ./
# test-app.html и иконки копируем из стадии build
COPY --from=build /app/test-app.html ./
# Иконки копируем из стадии build (если есть)
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

