# Скрипт для сборки и подготовки к развертыванию
Write-Host "=== Сборка сервера Techapp для развертывания ===" -ForegroundColor Green
Write-Host ""

# Проверяем, что мы в правильной директории
if (-not (Test-Path "server")) {
    Write-Host "Ошибка: папка server не найдена!" -ForegroundColor Red
    Write-Host "Запустите скрипт из корневой директории проекта" -ForegroundColor Yellow
    exit 1
}

Write-Host "Шаг 1: Сборка JAR файла..." -ForegroundColor Cyan
& .\gradlew.bat :server:shadowJar

if ($LASTEXITCODE -ne 0) {
    Write-Host "Ошибка при сборке!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ JAR файл собран успешно" -ForegroundColor Green
Write-Host ""

# Проверяем наличие JAR файла
$jarPath = "server\build\libs\server-all.jar"
if (Test-Path $jarPath) {
    $jarSize = (Get-Item $jarPath).Length / 1MB
    Write-Host "Файл: $jarPath" -ForegroundColor Cyan
    Write-Host "Размер: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host "Ошибка: JAR файл не найден!" -ForegroundColor Red
    exit 1
}

Write-Host "Шаг 2: Проверка Dockerfile..." -ForegroundColor Cyan
if (Test-Path "Dockerfile") {
    Write-Host "✓ Dockerfile найден" -ForegroundColor Green
} else {
    Write-Host "⚠ Dockerfile не найден, но это не критично" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Готово к развертыванию! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Следующие шаги:" -ForegroundColor Yellow
Write-Host "1. Загрузите проект на GitHub (если еще не загружен)" -ForegroundColor White
Write-Host "2. Зарегистрируйтесь на Render.com или другом хостинге" -ForegroundColor White
Write-Host "3. Создайте Web Service и подключите репозиторий" -ForegroundColor White
Write-Host "4. Настройте переменные окружения:" -ForegroundColor White
Write-Host "   - PORT=8081" -ForegroundColor Gray
Write-Host "   - HOST=0.0.0.0" -ForegroundColor Gray
Write-Host ""
Write-Host "Подробная инструкция: см. DEPLOYMENT_GUIDE.md" -ForegroundColor Cyan
Write-Host ""

