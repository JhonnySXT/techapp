package com.techapp.server.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.techapp.server.database.Users
import com.techapp.server.models.AuthTokens
import com.techapp.server.models.UserRole
import com.techapp.server.models.UserSummaryDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.*

object AuthService {
    private val secret = System.getenv("JWT_SECRET") ?: "techapp-secret-key-change-in-production"
    private val algorithm = Algorithm.HMAC256(secret)
    private val issuer = "techapp"
    private val accessTokenExpiry = 24 * 60 * 60 * 1000L // 24 часа
    private val refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L // 7 дней

    fun login(login: String, password: String): AuthTokens? {
        return org.jetbrains.exposed.sql.transactions.transaction {
            // Ищем пользователя по имени (фамилия и инициалы)
            val user = Users.select { Users.name eq login.trim() }.singleOrNull()
                ?: return@transaction null

            val passwordHash = user[Users.passwordHash]
            if (!BCrypt.checkpw(password, passwordHash)) {
                return@transaction null
            }

            val userId = user[Users.id].value.toString()
            val userName = user[Users.name]
            val userRole = UserRole.valueOf(user[Users.role])

            val accessToken = generateAccessToken(userId, userRole)
            val refreshToken = generateRefreshToken(userId)
            
            // Обновляем время последней активности при входе
            com.techapp.server.services.UserService.updateLastSeen(userId)

            AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = UserSummaryDto(
                    id = userId,
                    name = userName,
                    role = userRole,
                    isOnline = true
                )
            )
        }
    }

    fun verifyToken(token: String): UserSummaryDto? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
            val decoded = verifier.verify(token)
            val userId = decoded.subject
            val role = UserRole.valueOf(decoded.getClaim("role").asString())

            org.jetbrains.exposed.sql.transactions.transaction {
                val userUuid = try {
                    UUID.fromString(userId)
                } catch (e: Exception) {
                    return@transaction null
                }
                val user = Users.select { Users.id eq userUuid }.singleOrNull()
                    ?: return@transaction null
                UserSummaryDto(
                    id = userId,
                    name = user[Users.name],
                    role = role
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateAccessToken(userId: String, role: UserRole): String {
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiry))
            .sign(algorithm)
    }

    private fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId)
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiry))
            .sign(algorithm)
    }
}



