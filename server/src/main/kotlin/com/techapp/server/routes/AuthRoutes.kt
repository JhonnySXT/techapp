package com.techapp.server.routes

import com.techapp.server.models.AuthTokens
import com.techapp.server.models.LoginRequest
import com.techapp.server.services.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            try {
                val payload = runCatching { call.receive<LoginRequest>() }.getOrNull()
                if (payload == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Неверный формат данных"))
                    return@post
                }
                
                val tokens = try {
                    AuthService.login(payload.login, payload.password)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Ошибка сервера при авторизации")
                    )
                    return@post
                }
                
                if (tokens == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Неверный логин или пароль"))
                    return@post
                }
                
                call.respond(tokens)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Внутренняя ошибка сервера")
                )
            }
        }
    }
}

