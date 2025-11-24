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

object WebSocketManager {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    
    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.getOrPut(userId) { mutableSetOf() }.add(session)
    }
    
    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
    }
    
    suspend fun notifyTicketUpdate(ticket: TicketDto, targetUserIds: List<String>) {
        val message = Json.encodeToString(ticket)
        targetUserIds.forEach { userId ->
            connections[userId]?.forEach { session ->
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    // Игнорируем ошибки отправки
                }
            }
        }
    }
}

fun Application.configureWebSocket() {
    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        webSocket("/ws") {
            val token = call.request.queryParameters["token"]
            if (token == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Токен не предоставлен"))
                return@webSocket
            }
            
            val user = AuthService.verifyToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Неверный токен"))
                return@webSocket
            }
            
            WebSocketManager.addConnection(user.id, this)
            
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        // Эхо для проверки соединения
                        send(Frame.Text("pong"))
                    }
                }
            } finally {
                WebSocketManager.removeConnection(user.id, this)
            }
        }
    }
}



