// Конфигурация приложения для dev/prod окружений
(function() {
    'use strict';
    
    // Автоматическое определение окружения
    const isProduction = window.location.protocol === 'https:' || 
                        window.location.hostname !== 'localhost' && 
                        window.location.hostname !== '127.0.0.1';
    
    // Базовый URL API
    // Для production: используем тот же домен, что и фронтенд
    // Для development: localhost:8081
    const getApiBase = () => {
        if (isProduction) {
            // В production API находится на том же домене
            const protocol = window.location.protocol;
            const hostname = window.location.hostname;
            // Если фронтенд на Render, API тоже на Render (тот же домен)
            return `${protocol}//${hostname}/api/v1`;
        } else {
            // Development: localhost
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
    
    // WebSocket URL с правильной обработкой протокола и порта
    const getWebSocketUrl = () => {
        if (isProduction) {
            // В production используем тот же протокол и хост, что и для основного запроса
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const hostname = window.location.hostname;
            const port = window.location.port ? `:${window.location.port}` : '';
            return `${protocol}//${hostname}${port}/ws`;
        } else {
            // Development: localhost
            return 'ws://localhost:8081/ws';
        }
    };
    
    // Экспортируем конфигурацию
    window.APP_CONFIG = {
        API_BASE: getApiBase(),
        FILE_BASE: getFileBase(),
        IS_PRODUCTION: isProduction,
        WS_URL: getWebSocketUrl()
    };
    
    console.log('📱 Конфигурация приложения:', {
        environment: isProduction ? 'PRODUCTION' : 'DEVELOPMENT',
        apiBase: window.APP_CONFIG.API_BASE,
        fileBase: window.APP_CONFIG.FILE_BASE,
        wsUrl: window.APP_CONFIG.WS_URL
    });
})();

