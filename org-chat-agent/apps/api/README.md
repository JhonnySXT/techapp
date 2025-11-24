# OrgChat API

NestJS 10 + Prisma + Socket.IO gateway.

## Что готово
- Health-check (`GET /health`) и корневой статус.
- Модуль каналов с REST (`GET /channels`, `GET /channels/:id/messages`, `POST /channels/:id/messages`).
- WebSocket gateway (`channels:join-all`, `message:send`, broadcast `message:new`).
- Prisma схема пользователей/каналов/сообщений.
- Dockerfile и команды для запуска через Compose.

## Локальный запуск
```bash
pnpm install
pnpm --filter org-chat-api start:dev
```

Перед запуском:
1. Скопируй `env.example -> .env` в корень монорепо и при необходимости обнови значения.
2. Подними Postgres/Redis (`docker-compose up db redis -d`) или используй внешние сервисы.
3. Выполни миграции и сид: `pnpm db:migrate && pnpm db:seed`.

## Следующие шаги
- Добавить auth (JWT + refresh) и middleware для REST + WS.
- Подключить Redis pub/sub вместо in-memory fallback.
- Реализовать полноценные CRUD-операции для каналов и участников.
