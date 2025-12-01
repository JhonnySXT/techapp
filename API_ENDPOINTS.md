# API Endpoints - Техническая служба "Дача"

## Базовый URL
```
http://localhost:8081
```

## Доступные эндпоинты

### Health Check
```
GET /health
```
**Пример:** `http://localhost:8081/health`
**Ответ:** `ok`

---

### Авторизация

#### Вход в систему
```
POST /api/v1/auth/login
Content-Type: application/json

Body:
{
  "login": "admin@techapp.local",
  "password": "admin123"
}
```

**Пример запроса (PowerShell):**
```powershell
$body = @{
    login = "admin@techapp.local"
    password = "admin123"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method POST -ContentType "application/json" -Body $body
```

**Ответ (успех):**
```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

---

### Пользователи (требуется роль ADMIN)

#### Список пользователей
```
GET /api/v1/users
Authorization: Bearer {accessToken}
```

#### Создать пользователя
```
POST /api/v1/users
Authorization: Bearer {accessToken}
Content-Type: application/json

Body:
{
  "email": "user@example.com",
  "password": "password123",
  "name": "Имя Фамилия",
  "role": "TECHNICIAN"
}
```

#### Удалить пользователя
```
DELETE /api/v1/users/{id}
Authorization: Bearer {accessToken}
```

---

### Заявки

#### Список заявок
```
GET /api/v1/tickets
Authorization: Bearer {accessToken}
```

#### Создать заявку (MANAGER/ADMIN)
```
POST /api/v1/tickets
Authorization: Bearer {accessToken}
Content-Type: application/json

Body:
{
  "title": "Название заявки",
  "description": "Описание проблемы",
  "priority": "MEDIUM",
  "location": "Местоположение"
}
```

#### Принять заявку (TECHNICIAN)
```
PUT /api/v1/tickets/{id}/accept
Authorization: Bearer {accessToken}
Content-Type: application/json

Body:
{
  "estimatedCompletionTime": 1234567890
}
```

#### Завершить заявку
```
PUT /api/v1/tickets/{id}/complete
Authorization: Bearer {accessToken}
Content-Type: application/json

Body:
{
  "comments": "Работа выполнена"
}
```

#### Экспорт в PDF (MANAGER/ADMIN)
```
GET /api/v1/tickets/export/pdf
Authorization: Bearer {accessToken}
```

---

### WebSocket

#### Подключение
```
WS /ws?token={accessToken}
```

**Пример (JavaScript):**
```javascript
const socket = new WebSocket('ws://localhost:8081/ws?token=YOUR_ACCESS_TOKEN');
```

---

## Тестирование через браузер

⚠️ **Важно:** Большинство эндпоинтов требуют POST запросы и авторизацию. В браузере можно открыть только:

1. **Health check:** `http://localhost:8081/health`
2. **GET эндпоинты** (требуют токен в заголовке)

Для тестирования API используйте:
- **Postman** (рекомендуется)
- **PowerShell** (Invoke-RestMethod)
- **curl**
- **JavaScript fetch**

---

## Примеры использования

### 1. Получить токен доступа
```powershell
$loginBody = @{
    login = "admin@techapp.local"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$token = $response.accessToken
```

### 2. Получить список заявок
```powershell
$headers = @{
    Authorization = "Bearer $token"
}

$tickets = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/tickets" `
    -Method GET `
    -Headers $headers

$tickets | ConvertTo-Json
```

---

## Роли пользователей

- **ADMIN** - полный доступ ко всем функциям
- **MANAGER** - может создавать заявки, назначать техников, экспортировать PDF
- **TECHNICIAN** - может принимать и завершать заявки

---

## Коды ответов

- `200 OK` - успешный запрос
- `400 Bad Request` - неверный формат данных
- `401 Unauthorized` - требуется авторизация или неверный токен
- `403 Forbidden` - недостаточно прав
- `404 Not Found` - эндпоинт не найден
- `500 Internal Server Error` - ошибка сервера

