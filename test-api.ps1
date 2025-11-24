# Простой скрипт для тестирования API

Write-Host "=== Тестирование API Загородного клуба 'Дача' ===" -ForegroundColor Cyan

# 1. Health check
Write-Host "`n1. Проверка health endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8081/health" -Method Get
    Write-Host "   ✅ Health: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Health не работает: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# 2. Авторизация
Write-Host "`n2. Тест авторизации..." -ForegroundColor Yellow
$loginBody = @{
    email = "admin@techapp.local"
    password = "admin123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "   ✅ Авторизация успешна!" -ForegroundColor Green
    Write-Host "   Пользователь: $($response.user.name) ($($response.user.role))" -ForegroundColor Cyan
    $token = $response.accessToken
    Write-Host "   Токен получен (первые 30 символов): $($token.Substring(0, [Math]::Min(30, $token.Length)))..." -ForegroundColor Gray
    
    # 3. Получение списка пользователей
    Write-Host "`n3. Тест получения списка пользователей..." -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    try {
        $users = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/users" -Method Get -Headers $headers
        Write-Host "   ✅ Получено пользователей: $($users.items.Count)" -ForegroundColor Green
        foreach ($user in $users.items) {
            Write-Host "      - $($user.name) ($($user.role))" -ForegroundColor Gray
        }
    } catch {
        Write-Host "   ❌ Ошибка получения пользователей: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # 4. Получение списка заявок
    Write-Host "`n4. Тест получения списка заявок..." -ForegroundColor Yellow
    try {
        $tickets = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/tickets" -Method Get -Headers $headers
        Write-Host "   ✅ Получено заявок: $($tickets.items.Count)" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Ошибка получения заявок: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "   ❌ Ошибка авторизации: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Статус код: $statusCode" -ForegroundColor Red
    }
}

Write-Host "`n=== Тестирование завершено ===" -ForegroundColor Cyan



