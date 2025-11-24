Param(
    [string]$WorkspaceName = "org-chat-agent",
    [switch]$OneShot
)

$ErrorActionPreference = "Stop"

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] [CommsLink/$Level] $Message"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$workspacePath = Join-Path $repoRoot $WorkspaceName

if (-not (Test-Path $workspacePath)) {
    Write-Log "Creating workspace at $workspacePath"
    New-Item -ItemType Directory -Path $workspacePath | Out-Null
}

function Ensure-File {
    param(
        [string]$Path,
        [string]$Content
    )

    if (-not (Test-Path $Path)) {
        $dir = Split-Path $Path -Parent
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        Set-Content -Path $Path -Value $Content -Encoding UTF8
        Write-Log "Created $Path"
    } else {
        Write-Log "Skip existing file $Path" "DEBUG"
    }
}

Write-Log "Bootstrapping monorepo skeleton"

$folders = @(
    "apps/web",
    "apps/api",
    "packages/ui",
    "packages/config"
)

foreach ($folder in $folders) {
    $fullPath = Join-Path $workspacePath $folder
    if (-not (Test-Path $fullPath)) {
        New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        Write-Log "Created folder $folder"
    } else {
        Write-Log "Folder already exists $folder" "DEBUG"
    }
}

$packageJson = @{
    name = "org-chat"
    private = $true
    version = "0.0.1"
    packageManager = "pnpm@8.15.0"
    scripts = @{
        dev = "echo CommsLink: dev scripts pending"
        lint = "echo CommsLink: lint pending"
        test = "echo CommsLink: test pending"
    }
    workspaces = @("apps/*", "packages/*")
} | ConvertTo-Json -Depth 5

Ensure-File -Path (Join-Path $workspacePath "package.json") -Content $packageJson

$pnpmWorkspace = @'
packages:
  - apps/*
  - packages/*
'@
Ensure-File -Path (Join-Path $workspacePath "pnpm-workspace.yaml") -Content $pnpmWorkspace

$gitignore = @'
node_modules
.next
dist
.env
.env.*
.turbo
'@
Ensure-File -Path (Join-Path $workspacePath ".gitignore") -Content $gitignore

$dockerCompose = @'
services:
  api:
    build: ./apps/api
    ports:
      - "4000:4000"
    env_file:
      - .env
  web:
    build: ./apps/web
    ports:
      - "3000:3000"
    env_file:
      - .env
  db:
    image: postgres:16
    restart: unless-stopped
    environment:
      POSTGRES_USER: orgchat
      POSTGRES_PASSWORD: orgchat
      POSTGRES_DB: orgchat
    volumes:
      - db-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  redis:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  db-data:
'@
Ensure-File -Path (Join-Path $workspacePath "docker-compose.yaml") -Content $dockerCompose

$envExample = @'
NODE_ENV=development
API_PORT=4000
WEB_PORT=3000
DATABASE_URL=postgresql://orgchat:orgchat@db:5432/orgchat
REDIS_URL=redis://redis:6379
JWT_SECRET=replace-me
'@
Ensure-File -Path (Join-Path $workspacePath ".env.example") -Content $envExample

$webReadme = @'
# OrgChat Web

Next.js client placeholder.
Will handle auth flow, channel list, chat room UI and Socket.IO client.

Actual app bootstrap coming next.
'@
Ensure-File -Path (Join-Path $workspacePath "apps/web/README.md") -Content $webReadme

$apiReadme = @'
# OrgChat API

NestJS + Prisma + Socket.IO gateway placeholder.

Goals:
- JWT auth
- Channel/message CRUD
- Redis pub/sub for realtime
'@
Ensure-File -Path (Join-Path $workspacePath "apps/api/README.md") -Content $apiReadme

Write-Log "Base layout ready."

if ($OneShot) {
    Write-Log "OneShot mode, exiting."
    return
}

Write-Log "Idling, waiting for the next directive."
while ($true) {
    Start-Sleep -Seconds 60
    Write-Log "Standing by."
}

