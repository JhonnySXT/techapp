# syntax=docker/dockerfile:1.6
FROM node:20-alpine AS base
WORKDIR /srv/app

ENV PNPM_HOME="/root/.local/share/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable

COPY package.json pnpm-workspace.yaml turbo.json ./
COPY apps/web/package.json ./apps/web/package.json
COPY apps/api/package.json ./apps/api/package.json
COPY packages/ui/package.json ./packages/ui/package.json
COPY packages/config/package.json ./packages/config/package.json

RUN pnpm install

COPY . .

RUN pnpm --filter org-chat-web build

EXPOSE 3000
CMD ["pnpm", "--filter", "org-chat-web", "start"]






