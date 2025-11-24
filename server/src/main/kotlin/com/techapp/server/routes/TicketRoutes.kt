package com.techapp.server.routes

import com.techapp.server.models.TicketPriority
import com.techapp.server.models.UserRole
import com.techapp.server.plugins.requireAnyRole
import com.techapp.server.services.AuthService
import com.techapp.server.services.TicketService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.auth.jwt.JWTPrincipal

fun Route.ticketRoutes() {
    route("/tickets") {
        authenticate("jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                val role = principal?.payload?.getClaim("role")?.asString()?.let { UserRole.valueOf(it) }
                
                val tickets = TicketService.listTickets(userId, role)
                call.respond(mapOf("items" to tickets))
            }
            
            post {
                call.requireAnyRole(UserRole.MANAGER, UserRole.DIRECTOR, UserRole.ADMIN)
                
                val payload = runCatching { call.receive<TicketCreateRequest>() }.getOrNull()
                if (payload == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Неверный формат заявки"))
                    return@post
                }
                
                val principal = call.principal<JWTPrincipal>()
                val creatorId = principal?.payload?.subject ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Не авторизован"))
                    return@post
                }
                
                val ticket = TicketService.createTicket(
                    title = payload.title,
                    description = payload.description,
                    priority = payload.priority ?: TicketPriority.MEDIUM,
                    creatorId = creatorId,
                    assigneeId = payload.assigneeId,
                    deadlineAt = payload.deadlineAt,
                    photos = payload.photos ?: emptyList()
                )
                
                if (ticket == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Ошибка создания заявки. Возможно, пользователь-создатель не найден в базе данных или указан неверный ID техника."))
                    return@post
                }
                
                call.respond(HttpStatusCode.Created, ticket)
            }
            
            put("/{id}/accept") {
                call.requireAnyRole(UserRole.TECHNICIAN)
                
                val principal = call.principal<JWTPrincipal>()
                val technicianId = principal?.payload?.subject ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Не авторизован"))
                    return@put
                }
                
                val ticketId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID заявки не указан"))
                    return@put
                }
                
                // Пытаемся получить запрос, но если его нет или он пустой - это нормально (estimatedCompletionTime необязателен)
                val estimatedCompletionTime = try {
                    val request = call.receive<com.techapp.server.models.AcceptTicketRequest>()
                    request.estimatedCompletionTime
                } catch (e: Exception) {
                    // Если не удалось прочитать запрос, считаем что estimatedCompletionTime не указан
                    null
                }
                
                val ticket = TicketService.acceptTicket(ticketId, technicianId, estimatedCompletionTime)
                if (ticket == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Не удалось принять заявку. Возможно, заявка уже принята или имеет неверный статус."))
                    return@put
                }
                
                call.respond(ticket)
            }
            
            put("/{id}/assign") {
                call.requireAnyRole(UserRole.MANAGER, UserRole.DIRECTOR, UserRole.ADMIN)
                
                val principal = call.principal<JWTPrincipal>()
                val managerId = principal?.payload?.subject ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Не авторизован"))
                    return@put
                }
                
                val ticketId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID заявки не указан"))
                    return@put
                }
                
                val request = runCatching { call.receive<com.techapp.server.models.AssignTicketRequest>() }.getOrNull()
                if (request == null || request.technicianId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Не указан техник"))
                    return@put
                }
                
                val ticket = TicketService.assignTicket(ticketId, request.technicianId, managerId)
                if (ticket == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Не удалось назначить техника. Возможно, заявка или техник не найдены, или техник не является техником."))
                    return@put
                }
                
                call.respond(ticket)
            }
            
            put("/{id}/complete") {
                call.requireAnyRole(UserRole.TECHNICIAN)
                
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Не авторизован"))
                    return@put
                }
                
                val ticketId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID заявки не указан"))
                    return@put
                }
                
                val request = runCatching { call.receive<com.techapp.server.models.CompleteTicketRequest>() }.getOrNull()
                
                val ticket = TicketService.completeTicket(ticketId, request?.comments)
                if (ticket == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Не удалось завершить заявку"))
                    return@put
                }
                
                call.respond(ticket)
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class TicketCreateRequest(
    val title: String,
    val description: String,
    val priority: TicketPriority? = null,
    val assigneeId: String? = null,
    val deadlineAt: Long? = null,
    val photos: List<String>? = null // Список путей к фотографиям
)

