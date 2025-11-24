package com.techapp.server.database

import com.techapp.server.models.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.UUID

object InitUsers {
    fun initUsers() {
        transaction {
            // Сначала удаляем все заявки, чтобы избежать проблем с внешними ключами
            val allTicketIds = Tickets.selectAll().map { it[Tickets.id].value }
            allTicketIds.forEach { ticketId ->
                Tickets.deleteWhere { Tickets.id eq ticketId }
            }
            
            // Затем удаляем всех существующих пользователей
            val allUserIds = Users.selectAll().map { it[Users.id].value }
            allUserIds.forEach { userId ->
                Users.deleteWhere { Users.id eq userId }
            }
            
            val now = LocalDateTime.now()
            
            // Администратор
            Users.insert {
                it[email] = "bogdanov.ei@techapp.local"
                it[passwordHash] = BCrypt.hashpw("qerTY123", BCrypt.gensalt())
                it[name] = "Богданов Е.И."
                it[role] = UserRole.ADMIN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            // Руководители
            Users.insert {
                it[email] = "samarin.el@techapp.local"
                it[passwordHash] = BCrypt.hashpw("qwerty", BCrypt.gensalt())
                it[name] = "Самарин Э.Л."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "mulyk.im@techapp.local"
                it[passwordHash] = BCrypt.hashpw("supwaz", BCrypt.gensalt())
                it[name] = "Мулык И.М."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "petrova.vv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("petrow", BCrypt.gensalt())
                it[name] = "Петрова В.В."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            // Директор
            Users.insert {
                it[email] = "dvoyakovskaya.nv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("654321", BCrypt.gensalt())
                it[name] = "Двояковская Н.В."
                it[role] = UserRole.DIRECTOR.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            // Техники
            Users.insert {
                it[email] = "ananev.mo@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Ананьев М.О."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "safonov.vp@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Сафонев В.П."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "begdaev.ea@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Бегдаев Е.А."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "muratov.vv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Муратов В.В."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            Users.insert {
                it[email] = "bogdanov.an@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Богданов А.Н."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }
}

