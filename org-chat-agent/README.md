# OrgChat Agent (Codename: CommsLink)

Этот агент отвечает за разработку веб-приложения чата для внутренней организации. Он работает автономно, но под моим руководством. Ты взаимодействуешь только со мной, а CommsLink выполняет техническую работу.

## Миссия
- Создать защищённый чат для сотрудников с комнатами, личными диалогами и базовой модерацией.
- Обеспечить работу в браузере и подготовить API, чтобы позже подключить мобильных клиентов.

## Технологический набор
- **Фронтенд:** Next.js 14 (App Router), TypeScript, Tailwind, Zustand для локального состояния, Socket.IO client.
- **Бэкенд:** NestJS + Socket.IO gateway, Prisma (PostgreSQL) для хранения сообщений/чатов/пользователей, JWT авторизация, Redis для pub/sub.
- **Инфра:** Docker Compose (web + api + db + redis), Playwright для e2e, GitHub Actions (lint/test/build).

## Базовый бэклог
1. **Foundation**
   - Настроить монорепозиторий (`org-chat/`).
   - Докер-компоуз с Nest, Next, PostgreSQL, Redis.
   - Общие конфиги: ESLint/Prettier, Husky.
2. **Auth & Users**
   - Prisma схема пользователей, миграции.
   - Nest модуль auth (signup/login/refresh).
   - Next страницы входа/регистрации.
3. **Messaging Core**
   - Сущности `channels`, `memberships`, `messages`.
   - REST (создание каналов, список, история сообщений).
   - Socket.IO: получение/отправка сообщений в реальном времени.
   - UI: список каналов, чат-комната, индикатор онлайн.
4. **Enhancements**
   - Поиск по истории, вложения (S3-совместимое хранилище).
   - Нотификации (push/email), роль модератора.
   - PWA + офлайн очередь отправки.

## Следующие шаги для агента
1. Создать базовую структуру монорепозитория с пакетом `package.json` и workspace-ами (`apps/web`, `apps/api`, `packages/ui`).
2. Инициализировать NestJS и Next.js приложения (можно через `pnpm dlx`).
3. Подготовить Docker Compose и `.env.example`.
4. Настроить общие инструменты (ESLint, Prettier, Husky, lint-staged).

## Текущий прогресс
- [x] Монорепозиторий на `pnpm` с `turbo`, Husky и общими конфигами (`packages/config`).
- [x] Общая UI-библиотека `@org-chat/ui` с первыми компонентами.
- [x] Next.js 14 клиент с Tailwind, Zustand, Socket.IO-клиентом и базовым интерфейсом каналов.
- [x] NestJS API с Prisma схемой, REST + WebSocket каркасом, fallback данными и Dockerfile.
- [x] Docker Compose для web/api/db/redis, инструкция по переменным окружения (`env.example`).

Дальше: провести реальные миграции Prisma, добавить auth, хранение сообщений в Postgres и интеграцию с Redis pub/sub.

## Быстрый старт
1. Установить pnpm (>=8.15) и Node 18+.
2. `cp env.example .env` и при необходимости скорректировать переменные (`DATABASE_URL`, `CORS_ORIGIN` и т. д.).
3. `pnpm install`
4. `pnpm db:migrate && pnpm db:seed` — создадут таблицы и базовые каналы/сообщения.
5. `pnpm dev` — запустит web+api через Turbo (или `docker-compose up --build` для полного стека).

