package com.techapp.server.services

import com.techapp.server.database.Tickets
import com.techapp.server.database.Users
import com.techapp.server.models.*
import com.techapp.server.plugins.WebSocketManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object TicketService {
    private val json = kotlinx.serialization.json.Json { encodeDefaults = true }
    private val logger = org.slf4j.LoggerFactory.getLogger(TicketService::class.java)
    
    // Helper функция для безопасного парсинга UUID
    private fun parseUuid(uuidString: String, context: String): UUID? {
        return try {
            UUID.fromString(uuidString)
        } catch (e: Exception) {
            logger.error("Ошибка парсинга UUID ($context): $uuidString", e)
            null
        }
    }
    
    // Helper функция для создания UserSummaryDto из Users
    private fun toUserSummaryDto(user: ResultRow): UserSummaryDto {
        return UserSummaryDto(
            id = user[Users.id].value.toString(),
            name = user[Users.name],
            role = UserRole.valueOf(user[Users.role])
        )
    }
    
    // Helper функция для отправки уведомлений через WebSocket
    // Используем корутину без блокировки потока
    private fun notifyUsers(ticket: TicketDto, userIds: List<String>) {
        if (userIds.isEmpty()) return
        
        // Запускаем отправку уведомлений асинхронно, не блокируя основной поток
        // Используем runBlocking только если мы уже в синхронном контексте транзакции
        try {
            // В контексте транзакции Exposed мы не можем использовать suspend функции напрямую
            // Поэтому используем runBlocking, но это безопасно, так как мы вне транзакции
            runBlocking {
                WebSocketManager.notifyTicketUpdate(ticket, userIds)
            }
        } catch (e: Exception) {
            // Логируем ошибку, но не прерываем выполнение основной операции
            logger.warn("Ошибка отправки WebSocket уведомления для заявки ${ticket.id}: ${e.message}", e)
        }
    }
    
    // Получить список ID пользователей для уведомлений (менеджеры и админы)
    private fun getManagersAndAdminsIds(): List<String> {
        return transaction {
            Users.select {
                (Users.role eq UserRole.MANAGER.name) or
                (Users.role eq UserRole.DIRECTOR.name) or
                (Users.role eq UserRole.ADMIN.name)
            }.map { it[Users.id].value.toString() }
        }
    }
    
    fun createTicket(
        title: String,
        description: String,
        priority: TicketPriority,
        creatorId: String,
        assigneeId: String? = null,
        deadlineAt: Long? = null,
        photos: List<String> = emptyList()
    ): TicketDto? {
        return transaction {
            val creatorUuid = parseUuid(creatorId, "creatorId") ?: return@transaction null

            // Обрабатываем assigneeId: если пустая строка или null, то null
            val assigneeUuid = assigneeId?.takeIf { it.isNotBlank() }?.let { parseUuid(it, "assigneeId") }
            logger.info("Создание заявки: creatorId=$creatorId (UUID=$creatorUuid), assigneeId=$assigneeId (UUID=$assigneeUuid)")
            
            val creator = Users.select { Users.id eq creatorUuid }.singleOrNull()
            if (creator == null) {
                logger.error("Пользователь-создатель не найден в базе данных: UUID=$creatorUuid, creatorId=$creatorId")
                return@transaction null
            }
            logger.info("Пользователь-создатель найден: ${creator[Users.name]} (${creator[Users.role]})")

            // Проверяем assignee только если он указан и является техником
            val assignee = assigneeUuid?.let {
                val user = Users.select { Users.id eq it }.singleOrNull()
                if (user == null) {
                    return@transaction null // Техник не найден
                }
                // Проверяем, что это техник
                if (UserRole.valueOf(user[Users.role]) != UserRole.TECHNICIAN) {
                    return@transaction null // Указанный пользователь не является техником
                }
                user
            }

            val now = LocalDateTime.now()
            val nowInstant = Instant.now()
            // Сохраняем фотографии как JSON массив
            val photosJson = if (photos.isNotEmpty()) {
                try {
                    json.encodeToString(photos)
                } catch (e: Exception) {
                    null // Игнорируем ошибки сериализации фотографий
                }
            } else null
            
            val ticketId = Tickets.insert {
                it[Tickets.title] = title
                it[Tickets.description] = description
                it[Tickets.priority] = priority.name
                it[Tickets.status] = TicketStatus.NEW.name
                it[Tickets.creatorId] = creatorUuid
                it[Tickets.assigneeId] = assigneeUuid
                it[Tickets.createdAt] = now
                it[Tickets.updatedAt] = now
                it[Tickets.deadlineAt] = deadlineAt?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) }
                it[Tickets.photos] = photosJson
            } get Tickets.id

            val ticket = TicketDto(
                id = ticketId.value.toString(),
                title = title,
                description = description,
                priority = priority,
                status = TicketStatus.NEW,
                creator = UserSummaryDto(
                    id = creator[Users.id].value.toString(),
                    name = creator[Users.name],
                    role = UserRole.valueOf(creator[Users.role])
                ),
                assignee = assignee?.let {
                    UserSummaryDto(
                        id = it[Users.id].value.toString(),
                        name = it[Users.name],
                        role = UserRole.valueOf(it[Users.role])
                    )
                },
                createdAt = nowInstant.epochSecond,
                updatedAt = nowInstant.epochSecond,
                deadlineAt = deadlineAt,
                photos = photos
            )
            
            // Отправляем уведомления через WebSocket
            val notifyUserIds = mutableListOf(creatorId)
            assigneeId?.let { notifyUserIds.add(it) }
            notifyUserIds.addAll(getManagersAndAdminsIds())
            notifyUsers(ticket, notifyUserIds.distinct())
            
            ticket
        }
    }

    fun acceptTicket(ticketId: String, technicianId: String, estimatedCompletionTime: Long? = null): TicketDto? {
        return transaction {
            val ticketUuid = parseUuid(ticketId, "ticketId") ?: return@transaction null
            val techUuid = parseUuid(technicianId, "technicianId") ?: return@transaction null

            val ticketRow = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            val currentStatus = ticketRow[Tickets.status]
            val currentAssignee = ticketRow[Tickets.assigneeId]
            
            // Проверяем, что заявка может быть принята
            // NEW - новая заявка, может быть принята любым техником
            // ASSIGNED - назначенная заявка, может быть принята только назначенным техником
            when (currentStatus) {
                TicketStatus.NEW.name -> {
                    // Новая заявка - можно принять
                }
                TicketStatus.ASSIGNED.name -> {
                    // Проверяем, что заявка назначена именно этому технику
                    if (currentAssignee == null || currentAssignee.value != techUuid) {
                        // Заявка назначена другому технику или не назначена
                        return@transaction null
                    }
                }
                else -> {
                    // Заявка уже в работе, завершена или отменена
                    return@transaction null
                }
            }

            Tickets.update({ Tickets.id eq ticketUuid }) {
                it[Tickets.assigneeId] = techUuid
                it[Tickets.status] = TicketStatus.IN_PROGRESS.name
                it[Tickets.updatedAt] = LocalDateTime.now()
                it[Tickets.estimatedCompletionTime] = estimatedCompletionTime?.let { 
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) 
                }
            }

            val ticket = getTicketById(ticketId)
            
            // Отправляем уведомления через WebSocket
            ticket?.let { t ->
                val notifyUserIds = mutableListOf<String>()
                t.creator.id.let { notifyUserIds.add(it) }
                t.assignee?.id?.let { notifyUserIds.add(it) }
                notifyUserIds.addAll(getManagersAndAdminsIds())
                notifyUsers(t, notifyUserIds.distinct())
            }
            
            ticket
        }
    }

    fun assignTicket(ticketId: String, technicianId: String, assignedById: String): TicketDto? {
        return transaction {
            val ticketUuid = parseUuid(ticketId, "ticketId") ?: return@transaction null
            val techUuid = parseUuid(technicianId, "technicianId") ?: return@transaction null
            val managerUuid = parseUuid(assignedById, "assignedById") ?: return@transaction null

            // Проверяем, что заявка существует
            val ticketRow = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: run {
                    logger.warn("Заявка не найдена: $ticketId")
                    return@transaction null
                }

            // Проверяем, что техник существует и является техником
            val technician = Users.select { Users.id eq techUuid }.singleOrNull()
                ?: return@transaction null

            if (UserRole.valueOf(technician[Users.role]) != UserRole.TECHNICIAN) {
                return@transaction null
            }

            Tickets.update({ Tickets.id eq ticketUuid }) {
                it[Tickets.assigneeId] = techUuid
                it[Tickets.assignedById] = managerUuid
                it[Tickets.status] = TicketStatus.ASSIGNED.name
                it[Tickets.updatedAt] = LocalDateTime.now()
            }

            val ticket = getTicketById(ticketId)
            
            // Отправляем уведомления через WebSocket
            ticket?.let { t ->
                val notifyUserIds = mutableListOf<String>()
                t.creator.id.let { notifyUserIds.add(it) }
                t.assignee?.id?.let { notifyUserIds.add(it) }
                notifyUserIds.addAll(getManagersAndAdminsIds())
                notifyUsers(t, notifyUserIds.distinct())
            }
            
            ticket
        }
    }

    fun completeTicket(ticketId: String, comments: String? = null): TicketDto? {
        return transaction {
            val ticketUuid = parseUuid(ticketId, "ticketId") ?: return@transaction null

            val ticketRow = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            Tickets.update({ Tickets.id eq ticketUuid }) {
                it[Tickets.status] = TicketStatus.COMPLETED.name
                it[Tickets.updatedAt] = LocalDateTime.now()
                it[Tickets.completedAt] = LocalDateTime.now()
                it[Tickets.comments] = comments
            }

            val ticket = getTicketById(ticketId)
            
            // Отправляем уведомления через WebSocket
            ticket?.let { t ->
                val notifyUserIds = mutableListOf<String>()
                t.creator.id.let { notifyUserIds.add(it) }
                t.assignee?.id?.let { notifyUserIds.add(it) }
                notifyUserIds.addAll(getManagersAndAdminsIds())
                notifyUsers(t, notifyUserIds.distinct())
            }
            
            ticket
        }
    }

    fun getTicketById(ticketId: String): TicketDto? {
        return transaction {
            val ticketUuid = parseUuid(ticketId, "ticketId") ?: return@transaction null

            val ticket = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            // Оптимизация: загружаем всех нужных пользователей одним запросом
            val userIds = mutableSetOf<UUID>(ticket[Tickets.creatorId].value)
            ticket[Tickets.assigneeId]?.let { userIds.add(it.value) }
            ticket[Tickets.assignedById]?.let { userIds.add(it.value) }
            
            val usersMap = Users.select { Users.id inList userIds }.associateBy { it[Users.id].value }
            
            val creator = usersMap[ticket[Tickets.creatorId].value]
                ?: return@transaction null // Заявка с удалённым создателем
            val assignee = ticket[Tickets.assigneeId]?.let { usersMap[it.value] }
            val assignedBy = ticket[Tickets.assignedById]?.let { usersMap[it.value] }

            TicketDto(
                id = ticket[Tickets.id].value.toString(),
                title = ticket[Tickets.title],
                description = ticket[Tickets.description],
                priority = TicketPriority.valueOf(ticket[Tickets.priority]),
                status = TicketStatus.valueOf(ticket[Tickets.status]),
                creator = toUserSummaryDto(creator),
                assignee = assignee?.let { toUserSummaryDto(it) },
                assignedBy = assignedBy?.let { toUserSummaryDto(it) },
                createdAt = ticket[Tickets.createdAt].atZone(ZoneId.systemDefault()).toInstant().epochSecond,
                updatedAt = ticket[Tickets.updatedAt].atZone(ZoneId.systemDefault()).toInstant().epochSecond,
                completedAt = ticket[Tickets.completedAt]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                deadlineAt = ticket[Tickets.deadlineAt]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                estimatedCompletionTime = ticket[Tickets.estimatedCompletionTime]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                comments = ticket[Tickets.comments],
                photos = ticket[Tickets.photos]?.let { 
                    try {
                        json.decodeFromString<List<String>>(it)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            )
        }
    }

    fun listTickets(userId: String? = null, role: UserRole? = null, period: String? = null): List<TicketDto> {
        return transaction {
            val query = when {
                role == UserRole.ADMIN -> Tickets.selectAll()
                role == UserRole.MANAGER || role == UserRole.DIRECTOR -> Tickets.selectAll() // Менеджер и директор видят все заявки
                role == UserRole.TECHNICIAN && userId != null -> {
                    // Техники видят все заявки со статусом NEW или ASSIGNED (чтобы могли принять),
                    // а также заявки, где они назначены
                    val techUuid = parseUuid(userId, "userId") ?: return@transaction emptyList()
                    Tickets.select {
                        (Tickets.status eq TicketStatus.NEW.name) or
                        (Tickets.status eq TicketStatus.ASSIGNED.name) or
                        (Tickets.assigneeId.isNotNull() and (Tickets.assigneeId eq techUuid))
                    }
                }
                else -> Tickets.selectAll()
            }
            
            // Применяем фильтр по периоду для завершённых заявок
            val now = LocalDateTime.now()
            val periodStart = when (period) {
                "hour" -> now.minusHours(1)
                "day" -> now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                "month" -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                else -> null
            }

            // Оптимизация: загружаем всех пользователей одним запросом (избегаем N+1)
            val allUserIds = mutableSetOf<UUID>()
            query.forEach { row ->
                allUserIds.add(row[Tickets.creatorId].value)
                row[Tickets.assigneeId]?.let { allUserIds.add(it.value) }
                row[Tickets.assignedById]?.let { allUserIds.add(it.value) }
            }
            
            val usersMap = if (allUserIds.isNotEmpty()) {
                Users.select { Users.id inList allUserIds }.associateBy { it[Users.id].value }
            } else {
                emptyMap<UUID, ResultRow>()
            }
            
            query.mapNotNull { row ->
                // Если указан период, фильтруем по дате завершения
                if (periodStart != null && row[Tickets.completedAt] != null) {
                    val completedAt = row[Tickets.completedAt]!!
                    if (completedAt.isBefore(periodStart)) {
                        return@mapNotNull null
                    }
                }
                
                // Используем предзагруженных пользователей вместо отдельных запросов
                val creatorId = row[Tickets.creatorId].value
                val creator = usersMap[creatorId]
                    ?: return@mapNotNull null // Пропускаем заявки с удалёнными создателями
                
                val assignee = row[Tickets.assigneeId]?.let { assigneeId -> usersMap[assigneeId.value] }
                val assignedBy = row[Tickets.assignedById]?.let { assignedById -> usersMap[assignedById.value] }

                TicketDto(
                    id = row[Tickets.id].value.toString(),
                    title = row[Tickets.title],
                    description = row[Tickets.description],
                    priority = TicketPriority.valueOf(row[Tickets.priority]),
                    status = TicketStatus.valueOf(row[Tickets.status]),
                    creator = toUserSummaryDto(creator),
                    assignee = assignee?.let { toUserSummaryDto(it) },
                    assignedBy = assignedBy?.let { toUserSummaryDto(it) },
                    createdAt = row[Tickets.createdAt].atZone(ZoneId.systemDefault()).toInstant().epochSecond,
                    updatedAt = row[Tickets.updatedAt].atZone(ZoneId.systemDefault()).toInstant().epochSecond,
                    completedAt = row[Tickets.completedAt]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                    deadlineAt = row[Tickets.deadlineAt]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                    estimatedCompletionTime = row[Tickets.estimatedCompletionTime]?.atZone(ZoneId.systemDefault())?.toInstant()?.epochSecond,
                    comments = row[Tickets.comments],
                    photos = row[Tickets.photos]?.let { 
                        try {
                            json.decodeFromString<List<String>>(it)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()
                )
            }.sortedByDescending { it.createdAt }
        }
    }
}

