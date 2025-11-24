package com.techapp.server.services

import com.techapp.server.database.Users
import com.techapp.server.models.UserRole
import com.techapp.server.models.UserSummaryDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

object UserService {
    // Обновляет время последней активности пользователя
    fun updateLastSeen(userId: String) {
        try {
            transaction {
                val uuid = try {
                    UUID.fromString(userId)
                } catch (e: Exception) {
                    return@transaction
                }
                
                Users.update({ Users.id eq uuid }) {
                    it[Users.lastSeen] = LocalDateTime.now()
                }
            }
        } catch (e: Exception) {
            // Тихо игнорируем ошибки обновления lastSeen, чтобы не нарушать основной поток
        }
    }
    
    // Определяет, онлайн ли пользователь (был активен в последние 10 минут)
    private fun isUserOnline(lastSeen: LocalDateTime?): Boolean {
        if (lastSeen == null) return false
        val minutesAgo = java.time.Duration.between(lastSeen, LocalDateTime.now()).toMinutes()
        return minutesAgo <= 10 && minutesAgo >= 0
    }
    fun createUser(email: String, password: String, name: String, role: UserRole): UserSummaryDto? {
        return transaction {
            val existing = Users.select { Users.email eq email.lowercase() }.singleOrNull()
            if (existing != null) return@transaction null

            val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
            val now = LocalDateTime.now()

            val userId = Users.insert {
                it[Users.email] = email.lowercase()
                it[Users.passwordHash] = passwordHash
                it[Users.name] = name
                it[Users.role] = role.name
                it[Users.createdAt] = now
                it[Users.updatedAt] = now
            } get Users.id

            UserSummaryDto(
                id = userId.value.toString(),
                name = name,
                role = role
            )
        }
    }

    fun deleteUser(userId: String): Boolean {
        return transaction {
            val uuid = try {
                UUID.fromString(userId)
            } catch (e: Exception) {
                return@transaction false
            }

            val deleted = Users.deleteWhere { Users.id eq uuid }
            deleted > 0
        }
    }

    fun listUsers(): List<UserSummaryDto> {
        return transaction {
            val allUsers = Users.selectAll().map {
                val lastSeen = it[Users.lastSeen]
                val lastSeenEpoch = lastSeen?.atZone(java.time.ZoneId.systemDefault())?.toEpochSecond()
                val isOnline = isUserOnline(lastSeen)
                
                println("Пользователь ${it[Users.name]}: lastSeen=$lastSeen, isOnline=$isOnline")
                
                UserSummaryDto(
                    id = it[Users.id].value.toString(),
                    name = it[Users.name],
                    role = UserRole.valueOf(it[Users.role]),
                    lastSeen = lastSeenEpoch,
                    isOnline = isOnline
                )
            }
            println("Всего пользователей: ${allUsers.size}, онлайн: ${allUsers.count { it.isOnline }}")
            allUsers
        }
    }

    fun getUserById(userId: String): UserSummaryDto? {
        return transaction {
            val uuid = try {
                UUID.fromString(userId)
            } catch (e: Exception) {
                return@transaction null
            }

            Users.select { Users.id eq uuid }.singleOrNull()?.let {
                val lastSeen = it[Users.lastSeen]
                val lastSeenEpoch = lastSeen?.atZone(java.time.ZoneId.systemDefault())?.toEpochSecond()
                val isOnline = isUserOnline(lastSeen)
                
                UserSummaryDto(
                    id = it[Users.id].value.toString(),
                    name = it[Users.name],
                    role = UserRole.valueOf(it[Users.role]),
                    lastSeen = lastSeenEpoch,
                    isOnline = isOnline
                )
            }
        }
    }
}

