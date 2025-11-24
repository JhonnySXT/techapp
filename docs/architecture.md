## Техническая служба — архитектура

### 1. Обзор системы
Веб-приложение с адаптацией под Android (PWA), единый бекэнд и REST API + WebSocket. Решение предоставляет:
- аутентификацию (JWT) и контроль ролей (`ADMIN`, `MANAGER`, `TECHNICIAN`);
- управление пользователями администратором;
- создание и назначение заявок руководителями;
- прием/выполнение/закрытие заявок техниками с комментариями и вложениями;
- мониторинг статусов в реальном времени и push-уведомления;
- отчеты и экспорт истории в PDF/CSV.

### 2. Технологический стек
- **Фронтенд**: Next.js (React 18, App Router) + TypeScript, UI-библиотека MUI, React Query, Zustand/Redux Toolkit, PWA (service worker) + Firebase Cloud Messaging (push).
- **Бекэнд**: NestJS (Node.js 20, TypeScript), Fastify-адаптер, классические слои (modules/controllers/services). Socket.IO для real-time. PDFKit + Puppeteer для отчетов. BullMQ + Redis для фоновых задач (генерация PDF/уведомления).
- **База данных**: PostgreSQL 15 + Prisma ORM. Схемы: `users`, `tickets`, `ticket_messages`, `attachments`, `audit_logs`, `push_tokens`.
- **Инфраструктура**: Docker Compose (dev), GitHub Actions (CI: lint, тесты, билд), Render/Fly.io для продакшена. S3-совместимое хранилище (Backblaze/MinIO) для вложений и PDF.

### 3. Логическая модель данных
- `users`: `id`, `email`, `password_hash`, `role`, `name`, `phone`, `status`.
- `tickets`: `id`, `title`, `description`, `priority`, `status`, `creator_id` (manager), `assignee_id` (technician), `sla_due`, `resolved_at`, `created_at`.
- `ticket_messages`: `id`, `ticket_id`, `author_id`, `body`, `attachments`, `created_at`.
- `attachments`: `id`, `ticket_id`, `url`, `mime`, `size`.
- `audit_logs`: действия, автор, объект, метаданные.
- `push_tokens`: `user_id`, `token`, `platform`, `last_used_at`.

### 4. REST API (черновой набросок)
- `POST /auth/login` – вход, выдает пары access/refresh.
- `POST /auth/refresh`, `POST /auth/logout`.
- `GET /users` (admin), `POST /users`, `PATCH /users/:id`, `DELETE /users/:id`.
- `GET /tickets` (фильтры: статус, исполнитель, даты), `POST /tickets` (manager), `PATCH /tickets/:id` (статус, назначение), `POST /tickets/:id/messages`, `POST /tickets/:id/complete`.
- `GET /reports/tickets` – выгрузка (JSON/CSV).
- `POST /reports/tickets/pdf` – генерация PDF, возвращает ссылку/файл.

WebSocket каналы:
- `/ws/notifications` — персональные уведомления.
- `/ws/tickets/:id` — события конкретной заявки.

### 5. Ролевые сценарии
- **Администратор**: CRUD пользователей, просмотр всех заявок, настройка справочников.
- **Руководитель**: создает заявки, назначает, отслеживает, видит аналитику.
- **Техник**: принимает/отклоняет/выполняет, добавляет отчеты, закрывает заявки.

### 6. UX/PWA
- Стартовый экран → форма входа.
- Dashboard по роли (карточки KPI + списки заявок).
- Карточка заявки: статус, чат-комментарии, вложения, таймер SLA, кнопки `Принять`, `В работе`, `Завершить`.
- История + фильтры + экспорт (кнопка PDF).
- Offline-first: кэширование последнего списка заявок, очередь действий.

### 7. План реализации (MVP)
1. **Подготовка**: репозиторий (monorepo), Docker, Prettier/ESLint, Husky.
2. **Auth + Users**: Prisma schema, миграции, Nest модуль, Next.js страницы входа, защитные маршруты.
3. **Tickets Core**: CRUD + фильтры + фронтенд таблица/карточки, базовые уведомления (toast).
4. **Real-time**: Socket.IO, подписки по ролям, UI-обновления без перезагрузки.
5. **PDF/Reports**: шаблоны, генерация в очереди, возможность скачать/отправить на email.
6. **PWA + Push**: сервис-воркер, FCM токены, пуши "новая заявка".
7. **Тесты и hardening**: e2e (Playwright + MSW), нагрузочное тестирование, мониторинг.

### 8. Нефункциональные требования
- SLA API < 200 мс (95-й перцентиль) при 200 rps.
- Защита данных: HTTPS, rate limiting, логирование событий безопасности.
- Масштабируемость: Stateless backend + горизонтальное масштабирование, отдельная очередь для фоновых работ.

### 9. Следующие шаги
1. Настроить monorepo (`frontend/`, `backend/`, `infrastructure/`).
2. Инициализировать Next.js + NestJS + Prisma (SQLite для dev).
3. Реализовать базовые сущности: пользователи и заявки + аутентификация.
4. Собрать минимальный UI: вход, список заявок, карточка заявки.


