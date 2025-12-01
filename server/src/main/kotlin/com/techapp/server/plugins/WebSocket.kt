package com.techapp.server.plugins

import com.techapp.server.models.TicketDto
import com.techapp.server.services.AuthService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

object WebSocketManager {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)
    
    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.getOrPut(userId) { mutableSetOf() }.add(session)
        logger.info("WebSocket подключение добавлено для пользователя: $userId. Всего соединений: ${connections.values.sumOf { it.size }}")
    }
    
    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
        logger.debug("WebSocket соединение удалено для пользователя: $userId. Осталось соединений: ${connections.values.sumOf { it.size }}")
    }
    
    suspend fun notifyTicketUpdate(ticket: TicketDto, targetUserIds: List<String>) {
        if (targetUserIds.isEmpty()) {
            logger.debug("Нет получателей для уведомления о заявке ${ticket.id}")
            return
        }
        
        val message = Json.encodeToString(ticket)
        var successCount = 0
        var errorCount = 0
        
        targetUserIds.forEach { userId ->
            val userConnections = connections[userId] ?: emptySet()
            if (userConnections.isEmpty()) {
                logger.debug("Пользователь $userId не подключен к WebSocket")
                return@forEach
            }
            
            userConnections.forEach { session ->
                try {
                    // Проверяем, что соединение еще открыто и отправляем сообщение
                    session.send(Frame.Text(message))
                    successCount++
                } catch (e: Exception) {
                    logger.warn("Ошибка отправки WebSocket сообщения пользователю $userId: ${e.message}", e)
                    // Удаляем проблемное соединение
                    try {
                        removeConnection(userId, session)
                    } catch (ex: Exception) {
                        // Игнорируем ошибки при удалении
                    }
                    errorCount++
                }
            }
        }
        
        logger.info("WebSocket уведомление отправлено для заявки ${ticket.id}: успешно=$successCount, ошибок=$errorCount, получателей=${targetUserIds.size}")
    }
}

fun Application.configureWebSocket() {
    val logger = LoggerFactory.getLogger("WebSocket")
    
    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        webSocket("/ws") {
            try {
                val token = call.request.queryParameters["token"]
                if (token == null) {
                    logger.warn("WebSocket подключение отклонено: токен не предоставлен")
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Токен не предоставлен"))
                    return@webSocket
                }
                
                val user = AuthService.verifyToken(token)
                if (user == null) {
                    logger.warn("WebSocket подключение отклонено: неверный токен")
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Неверный токен"))
                    return@webSocket
                }
                
                logger.info("WebSocket подключение установлено для пользователя: ${user.name} (${user.id})")
                WebSocketManager.addConnection(user.id, this)
                
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            // Эхо для проверки соединения
                            try {
                                send(Frame.Text("pong"))
                            } catch (e: Exception) {
                                logger.warn("Ошибка отправки pong: ${e.message}")
                                throw e
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Ошибка в WebSocket соединении для пользователя ${user.id}: ${e.message}")
                } finally {
                    logger.info("WebSocket соединение закрыто для пользователя: ${user.name} (${user.id})")
                    WebSocketManager.removeConnection(user.id, this)
                }
            } catch (e: Exception) {
                logger.error("Критическая ошибка в WebSocket: ${e.message}", e)
                try {
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Внутренняя ошибка сервера"))
                } catch (ex: Exception) {
                    // Игнорируем ошибки при закрытии
                }
            }
        }
    }
}



