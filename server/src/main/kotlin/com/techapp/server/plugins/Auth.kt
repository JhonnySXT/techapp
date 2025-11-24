package com.techapp.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.techapp.server.models.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond

fun Application.configureAuth() {
    val secret = System.getenv("JWT_SECRET") ?: "techapp-secret-key-change-in-production"
    val issuer = "techapp"
    val algorithm = Algorithm.HMAC256(secret)

    install(Authentication) {
        jwt("jwt") {
            realm = issuer
            verifier(
                JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject
                val role = credential.payload.getClaim("role").asString()
                // Обновляем время последней активности при каждом запросе
                com.techapp.server.services.UserService.updateLastSeen(userId)
                JWTPrincipal(credential.payload)
            }
        }
    }
}

suspend fun ApplicationCall.requireRole(role: UserRole) {
    val principal = principal<JWTPrincipal>()
    val userRole = principal?.payload?.getClaim("role")?.asString()?.let { UserRole.valueOf(it) }
    
    if (userRole != role && userRole != UserRole.ADMIN) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Недостаточно прав"))
        return
    }
}

suspend fun ApplicationCall.requireAnyRole(vararg roles: UserRole) {
    val principal = principal<JWTPrincipal>()
    val userRole = principal?.payload?.getClaim("role")?.asString()?.let { UserRole.valueOf(it) }
    
    if (userRole == null || (!roles.contains(userRole) && userRole != UserRole.ADMIN)) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Недостаточно прав"))
        return
    }
}

