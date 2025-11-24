package com.techapp.server.routes

import com.techapp.server.models.UserRole
import com.techapp.server.plugins.requireRole
import com.techapp.server.plugins.requireAnyRole
import com.techapp.server.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userRoutes() {
    route("/users") {
        authenticate("jwt") {
            get {
                // Разрешаем доступ всем авторизованным пользователям для просмотра списка пользователей (нужно для "Сотрудники онлайн")
                // Администраторы, руководители, директора и техники могут видеть список
                call.requireAnyRole(UserRole.ADMIN, UserRole.MANAGER, UserRole.DIRECTOR, UserRole.TECHNICIAN)
                
                // Убеждаемся, что lastSeen обновлен для текущего пользователя
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal?.payload?.subject
                if (currentUserId != null) {
                    UserService.updateLastSeen(currentUserId)
                }
                
                val users = UserService.listUsers()
                call.respond(mapOf("items" to users))
            }
            
            post {
                call.requireRole(UserRole.ADMIN)
                val payload = runCatching { call.receive<UserCreateRequest>() }.getOrNull()
                if (payload == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Неверный формат данных"))
                    return@post
                }
                
                val user = UserService.createUser(
                    email = payload.email,
                    password = payload.password,
                    name = payload.name,
                    role = payload.role
                )
                
                if (user == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Пользователь с таким email уже существует"))
                    return@post
                }
                
                call.respond(HttpStatusCode.Created, user)
            }
            
            delete("/{id}") {
                call.requireRole(UserRole.ADMIN)
                val userId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID пользователя не указан"))
                    return@delete
                }
                
                val deleted = UserService.deleteUser(userId)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Пользователь не найден"))
                    return@delete
                }
                
                call.respond(HttpStatusCode.OK, mapOf("message" to "Пользователь удалён"))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class UserCreateRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole
)

