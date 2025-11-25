// Service Worker для офлайн-режима
const CACHE_NAME = 'techapp-cache-v1';
const API_CACHE_NAME = 'techapp-api-cache-v1';

// Файлы для кэширования
const STATIC_FILES = [
    '/',
    '/test-app.html',
    '/test-login.html',
    '/config.js',
    '/manifest.json',
    '/sw.js'
];

// Установка Service Worker
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(STATIC_FILES);
        })
    );
    self.skipWaiting();
});

// Активация Service Worker
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cacheName) => {
                    if (cacheName !== CACHE_NAME && cacheName !== API_CACHE_NAME) {
                        return caches.delete(cacheName);
                    }
                })
            );
        })
    );
    return self.clients.claim();
});

// Перехват запросов
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    
    // Кэшируем API запросы для офлайн-режима
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(
            caches.open(API_CACHE_NAME).then((cache) => {
                return fetch(event.request)
                    .then((response) => {
                        // Кэшируем только успешные GET запросы
                        if (event.request.method === 'GET' && response.ok) {
                            cache.put(event.request, response.clone());
                        }
                        return response;
                    })
                    .catch(() => {
                        // В офлайн-режиме возвращаем из кэша
                        return cache.match(event.request).then((cachedResponse) => {
                            if (cachedResponse) {
                                return cachedResponse;
                            }
                            // Если нет в кэше, возвращаем базовый ответ
                            return new Response(
                                JSON.stringify({ error: 'Офлайн режим. Данные недоступны.' }),
                                {
                                    status: 503,
                                    headers: { 'Content-Type': 'application/json' }
                                }
                            );
                        });
                    });
            })
        );
    } else {
        // Для статических файлов используем стратегию "сеть, затем кэш"
        event.respondWith(
            fetch(event.request)
                .then((response) => {
                    const responseClone = response.clone();
                    caches.open(CACHE_NAME).then((cache) => {
                        cache.put(event.request, responseClone);
                    });
                    return response;
                })
                .catch(() => {
                    return caches.match(event.request);
                })
        );
    }
});

// Обработка сообщений от основного потока
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

