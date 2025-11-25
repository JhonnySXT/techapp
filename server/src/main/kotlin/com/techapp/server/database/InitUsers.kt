package com.techapp.server.database

import com.techapp.server.models.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.UUID

object InitUsers {
    private val logger = org.slf4j.LoggerFactory.getLogger(InitUsers::class.java)
    
    fun initUsers() {
        transaction {
            logger.info("Начало инициализации пользователей...")
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
            
            // Администратор (используем фиксированный UUID для стабильности)
            val adminUuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
            Users.insert {
                it[id] = adminUuid
                it[email] = "bogdanov.ei@techapp.local"
                it[passwordHash] = BCrypt.hashpw("qerTY123", BCrypt.gensalt())
                it[name] = "Богданов Е.И."
                it[role] = UserRole.ADMIN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан администратор: Богданов Е.И. (UUID: $adminUuid)")
            
            // Руководители (фиксированные UUID)
            val manager1Uuid = UUID.fromString("00000000-0000-0000-0000-000000000002")
            Users.insert {
                it[id] = manager1Uuid
                it[email] = "samarin.el@techapp.local"
                it[passwordHash] = BCrypt.hashpw("qwerty", BCrypt.gensalt())
                it[name] = "Самарин Э.Л."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан руководитель: Самарин Э.Л. (UUID: $manager1Uuid)")
            
            val manager2Uuid = UUID.fromString("00000000-0000-0000-0000-000000000003")
            Users.insert {
                it[id] = manager2Uuid
                it[email] = "mulyk.im@techapp.local"
                it[passwordHash] = BCrypt.hashpw("supwaz", BCrypt.gensalt())
                it[name] = "Мулык И.М."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан руководитель: Мулык И.М. (UUID: $manager2Uuid)")
            
            val manager3Uuid = UUID.fromString("00000000-0000-0000-0000-000000000004")
            Users.insert {
                it[id] = manager3Uuid
                it[email] = "petrova.vv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("petrow", BCrypt.gensalt())
                it[name] = "Петрова В.В."
                it[role] = UserRole.MANAGER.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан руководитель: Петрова В.В. (UUID: $manager3Uuid)")
            
            // Директор (фиксированный UUID)
            val directorUuid = UUID.fromString("00000000-0000-0000-0000-000000000005")
            Users.insert {
                it[id] = directorUuid
                it[email] = "dvoyakovskaya.nv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("654321", BCrypt.gensalt())
                it[name] = "Двояковская Н.В."
                it[role] = UserRole.DIRECTOR.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан директор: Двояковская Н.В. (UUID: $directorUuid)")
            
            // Техники (фиксированные UUID)
            val tech1Uuid = UUID.fromString("00000000-0000-0000-0000-000000000010")
            Users.insert {
                it[id] = tech1Uuid
                it[email] = "ananev.mo@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Ананьев М.О."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан техник: Ананьев М.О. (UUID: $tech1Uuid)")
            
            val tech2Uuid = UUID.fromString("00000000-0000-0000-0000-000000000011")
            Users.insert {
                it[id] = tech2Uuid
                it[email] = "safonov.vp@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Сафонев В.П."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан техник: Сафонев В.П. (UUID: $tech2Uuid)")
            
            val tech3Uuid = UUID.fromString("00000000-0000-0000-0000-000000000012")
            Users.insert {
                it[id] = tech3Uuid
                it[email] = "begdaev.ea@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Бегдаев Е.А."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан техник: Бегдаев Е.А. (UUID: $tech3Uuid)")
            
            val tech4Uuid = UUID.fromString("00000000-0000-0000-0000-000000000013")
            Users.insert {
                it[id] = tech4Uuid
                it[email] = "muratov.vv@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Муратов В.В."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан техник: Муратов В.В. (UUID: $tech4Uuid)")
            
            val tech5Uuid = UUID.fromString("00000000-0000-0000-0000-000000000014")
            Users.insert {
                it[id] = tech5Uuid
                it[email] = "bogdanov.an@techapp.local"
                it[passwordHash] = BCrypt.hashpw("123456", BCrypt.gensalt())
                it[name] = "Богданов А.Н."
                it[role] = UserRole.TECHNICIAN.name
                it[createdAt] = now
                it[updatedAt] = now
            }
            logger.info("Создан техник: Богданов А.Н. (UUID: $tech5Uuid)")
            
            logger.info("Инициализация пользователей завершена успешно. Создано пользователей: ${Users.selectAll().count()}")
        }
    }
}

