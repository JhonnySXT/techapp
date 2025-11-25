/**
 * Тестовый скрипт для проверки основных функций приложения
 * Запуск: node test-app-functions.js
 */

const API_BASE = 'http://localhost:8081/api/v1';

// Тестовые данные
const TEST_USERS = {
    manager: { login: 'Петрова В.В.', password: 'petrow' },
    technician: { login: 'Ананьев М.О.', password: '123456' },
    admin: { login: 'Богданов Е.И.', password: 'qerTY123' }
};

let authToken = null;
let currentUser = null;

// Утилиты
async function fetchAPI(url, options = {}) {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
                ...(options.headers || {})
            }
        });
        
        const text = await response.text();
        let data = {};
        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            data = { error: 'Invalid JSON response', text };
        }
        
        return { response, data };
    } catch (error) {
        return { response: null, data: { error: error.message } };
    }
}

// Тесты
const tests = {
    passed: 0,
    failed: 0,
    results: []
};

function logTest(name, passed, message = '') {
    tests[passed ? 'passed' : 'failed']++;
    tests.results.push({ name, passed, message });
    const icon = passed ? '✅' : '❌';
    console.log(`${icon} ${name}${message ? ': ' + message : ''}`);
}

async function testHealthCheck() {
    const { response, data } = await fetchAPI(`${API_BASE.replace('/api/v1', '')}/health`);
    logTest('Health Check', response?.ok === true || response?.status === 200, 
        response?.ok ? 'Сервер работает' : `Статус: ${response?.status}`);
}

async function testLogin() {
    const { response, data } = await fetchAPI(`${API_BASE}/auth/login`, {
        method: 'POST',
        body: JSON.stringify(TEST_USERS.manager)
    });
    
    if (response?.ok && data.accessToken) {
        authToken = data.accessToken;
        currentUser = data.user;
        logTest('Авторизация', true, `Пользователь: ${data.user.name}`);
        return true;
    } else {
        logTest('Авторизация', false, data.error || 'Не удалось авторизоваться');
        return false;
    }
}

async function testLoadTickets() {
    const { response, data } = await fetchAPI(`${API_BASE}/tickets`);
    logTest('Загрузка заявок', response?.ok === true && Array.isArray(data.items),
        `Найдено заявок: ${data.items?.length || 0}`);
}

async function testLoadUsers() {
    const { response, data } = await fetchAPI(`${API_BASE}/users`);
    logTest('Загрузка пользователей', response?.ok === true && Array.isArray(data.items),
        `Найдено пользователей: ${data.items?.length || 0}`);
}

async function testCreateTicket() {
    const ticketData = {
        title: `Тестовая заявка ${Date.now()}`,
        description: 'Это тестовая заявка для проверки функциональности',
        priority: 'MEDIUM'
    };
    
    const { response, data } = await fetchAPI(`${API_BASE}/tickets`, {
        method: 'POST',
        body: JSON.stringify(ticketData)
    });
    
    if (response?.ok && data.id) {
        logTest('Создание заявки', true, `ID: ${data.id}`);
        return data.id;
    } else {
        logTest('Создание заявки', false, data.error || 'Не удалось создать заявку');
        return null;
    }
}

async function testAcceptTicket(ticketId) {
    if (!ticketId) {
        logTest('Принятие заявки', false, 'Нет ID заявки');
        return false;
    }
    
    // Сначала авторизуемся как техник
    const { response: loginResp, data: loginData } = await fetchAPI(`${API_BASE}/auth/login`, {
        method: 'POST',
        body: JSON.stringify(TEST_USERS.technician)
    });
    
    if (!loginResp?.ok) {
        logTest('Принятие заявки', false, 'Не удалось авторизоваться как техник');
        return false;
    }
    
    const techToken = loginData.accessToken;
    const { response, data } = await fetchAPI(`${API_BASE}/tickets/${ticketId}/accept`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${techToken}` },
        body: JSON.stringify({ estimatedCompletionTime: Math.floor(Date.now() / 1000) + 3600 })
    });
    
    logTest('Принятие заявки', response?.ok === true, 
        response?.ok ? 'Заявка принята' : (data.error || 'Ошибка'));
    return response?.ok;
}

async function testCompleteTicket(ticketId) {
    if (!ticketId) {
        logTest('Завершение заявки', false, 'Нет ID заявки');
        return false;
    }
    
    // Используем токен техника
    const { response: loginResp, data: loginData } = await fetchAPI(`${API_BASE}/auth/login`, {
        method: 'POST',
        body: JSON.stringify(TEST_USERS.technician)
    });
    
    if (!loginResp?.ok) {
        logTest('Завершение заявки', false, 'Не удалось авторизоваться как техник');
        return false;
    }
    
    const techToken = loginData.accessToken;
    const { response, data } = await fetchAPI(`${API_BASE}/tickets/${ticketId}/complete`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${techToken}` },
        body: JSON.stringify({ comments: 'Тестовое завершение заявки' })
    });
    
    logTest('Завершение заявки', response?.ok === true,
        response?.ok ? 'Заявка завершена' : (data.error || 'Ошибка'));
    return response?.ok;
}

async function testAssignTicket(ticketId) {
    if (!ticketId) {
        logTest('Назначение техника', false, 'Нет ID заявки');
        return false;
    }
    
    // Авторизуемся как менеджер
    const { response: loginResp, data: loginData } = await fetchAPI(`${API_BASE}/auth/login`, {
        method: 'POST',
        body: JSON.stringify(TEST_USERS.manager)
    });
    
    if (!loginResp?.ok) {
        logTest('Назначение техника', false, 'Не удалось авторизоваться как менеджер');
        return false;
    }
    
    const managerToken = loginData.accessToken;
    
    // Получаем список техников
    const { response: usersResp, data: usersData } = await fetchAPI(`${API_BASE}/users`, {
        headers: { 'Authorization': `Bearer ${managerToken}` }
    });
    
    if (!usersResp?.ok || !Array.isArray(usersData.items)) {
        logTest('Назначение техника', false, 'Не удалось загрузить список техников');
        return false;
    }
    
    const technician = usersData.items.find(u => u.role === 'TECHNICIAN');
    if (!technician) {
        logTest('Назначение техника', false, 'Нет доступных техников');
        return false;
    }
    
    const { response, data } = await fetchAPI(`${API_BASE}/tickets/${ticketId}/assign`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${managerToken}` },
        body: JSON.stringify({ technicianId: technician.id })
    });
    
    logTest('Назначение техника', response?.ok === true,
        response?.ok ? `Назначен: ${technician.name}` : (data.error || 'Ошибка'));
    return response?.ok;
}

async function testPDFExport() {
    const { response, data } = await fetchAPI(`${API_BASE}/tickets/export/pdf?period=day`);
    logTest('Экспорт PDF', response?.ok === true && response?.headers?.get('content-type')?.includes('pdf'),
        response?.ok ? 'PDF создан' : (data.error || 'Ошибка'));
}

async function testOfflineMode() {
    // Проверяем наличие IndexedDB
    const hasIndexedDB = typeof indexedDB !== 'undefined';
    logTest('Поддержка IndexedDB', hasIndexedDB, 
        hasIndexedDB ? 'Доступна' : 'Недоступна');
    
    // Проверяем наличие Service Worker
    const hasServiceWorker = typeof navigator !== 'undefined' && 'serviceWorker' in navigator;
    logTest('Поддержка Service Worker', hasServiceWorker,
        hasServiceWorker ? 'Доступна' : 'Недоступна');
}

async function testNotifications() {
    const hasNotifications = typeof Notification !== 'undefined';
    logTest('Поддержка уведомлений', hasNotifications,
        hasNotifications ? 'Доступна' : 'Недоступна');
}

// Основная функция запуска тестов
async function runTests() {
    console.log('🧪 Запуск тестов приложения...\n');
    
    // Проверка доступности сервера
    await testHealthCheck();
    
    if (!authToken) {
        const loggedIn = await testLogin();
        if (!loggedIn) {
            console.log('\n❌ Не удалось авторизоваться. Остановка тестов.');
            return;
        }
    }
    
    // Основные тесты
    await testLoadUsers();
    await testLoadTickets();
    
    // Тест создания заявки
    const ticketId = await testCreateTicket();
    
    // Тест назначения техника (если есть заявка)
    if (ticketId) {
        await testAssignTicket(ticketId);
    }
    
    // Тест принятия заявки (создаем новую для техника)
    const newTicketId = await testCreateTicket();
    if (newTicketId) {
        await testAcceptTicket(newTicketId);
    }
    
    // Тест завершения заявки
    if (newTicketId) {
        await testCompleteTicket(newTicketId);
    }
    
    // Тест экспорта PDF
    await testPDFExport();
    
    // Тесты офлайн-режима и уведомлений
    await testOfflineMode();
    await testNotifications();
    
    // Итоги
    console.log('\n' + '='.repeat(50));
    console.log(`📊 Итоги тестирования:`);
    console.log(`✅ Успешно: ${tests.passed}`);
    console.log(`❌ Ошибок: ${tests.failed}`);
    console.log(`📈 Успешность: ${Math.round((tests.passed / (tests.passed + tests.failed)) * 100)}%`);
    console.log('='.repeat(50));
    
    // Детальные результаты
    if (tests.failed > 0) {
        console.log('\n❌ Неудачные тесты:');
        tests.results.filter(r => !r.passed).forEach(r => {
            console.log(`  - ${r.name}: ${r.message || 'Ошибка'}`);
        });
    }
}

// Запуск тестов (только в Node.js окружении)
if (typeof fetch === 'undefined') {
    console.log('⚠️  Для запуска тестов требуется Node.js 18+ с поддержкой fetch API');
    console.log('   Или используйте браузерную консоль для ручного тестирования');
} else {
    runTests().catch(error => {
        console.error('❌ Критическая ошибка при запуске тестов:', error);
    });
}

