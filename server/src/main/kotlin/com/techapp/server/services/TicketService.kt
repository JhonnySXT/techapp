package com.techapp.server.services

import com.techapp.server.database.Tickets
import com.techapp.server.database.Users
import com.techapp.server.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

object TicketService {
    private val json = kotlinx.serialization.json.Json { encodeDefaults = true }
    
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
            val creatorUuid = try {
                UUID.fromString(creatorId)
            } catch (e: Exception) {
                return@transaction null
            }

            val assigneeUuid = assigneeId?.let {
                try {
                    UUID.fromString(it)
                } catch (e: Exception) {
                    return@transaction null
                }
            }

            val creator = Users.select { Users.id eq creatorUuid }.singleOrNull()
                ?: return@transaction null

            val assignee = assigneeUuid?.let {
                Users.select { Users.id eq it }.singleOrNull()
                    ?: return@transaction null
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

            TicketDto(
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
        }
    }

    fun acceptTicket(ticketId: String, technicianId: String, estimatedCompletionTime: Long? = null): TicketDto? {
        return transaction {
            val ticketUuid = try {
                UUID.fromString(ticketId)
            } catch (e: Exception) {
                return@transaction null
            }

            val techUuid = try {
                UUID.fromString(technicianId)
            } catch (e: Exception) {
                return@transaction null
            }

            val ticket = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            val currentStatus = ticket[Tickets.status]
            val currentAssignee = ticket[Tickets.assigneeId]
            
            // Проверяем, что заявка может быть принята
            // NEW - новая заявка, может быть принята любым техником
            // ASSIGNED - назначенная заявка, может быть принята только назначенным техником
            when (currentStatus) {
                TicketStatus.NEW.name -> {
                    // Новая заявка - можно принять
                }
                TicketStatus.ASSIGNED.name -> {
                    // Проверяем, что заявка назначена именно этому технику
                    if (currentAssignee == null || currentAssignee != techUuid) {
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

            getTicketById(ticketId)
        }
    }

    fun assignTicket(ticketId: String, technicianId: String, assignedById: String): TicketDto? {
        return transaction {
            val ticketUuid = try {
                UUID.fromString(ticketId)
            } catch (e: Exception) {
                println("Ошибка парсинга ticketId: $ticketId, ошибка: ${e.message}")
                return@transaction null
            }

            val techUuid = try {
                UUID.fromString(technicianId)
            } catch (e: Exception) {
                println("Ошибка парсинга technicianId: $technicianId, ошибка: ${e.message}")
                return@transaction null
            }

            val managerUuid = try {
                UUID.fromString(assignedById)
            } catch (e: Exception) {
                println("Ошибка парсинга assignedById: $assignedById, ошибка: ${e.message}")
                return@transaction null
            }

            // Проверяем, что заявка существует
            val ticket = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: run {
                    println("Заявка не найдена: $ticketId")
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

            getTicketById(ticketId)
        }
    }

    fun completeTicket(ticketId: String, comments: String? = null): TicketDto? {
        return transaction {
            val ticketUuid = try {
                UUID.fromString(ticketId)
            } catch (e: Exception) {
                return@transaction null
            }

            val ticket = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            Tickets.update({ Tickets.id eq ticketUuid }) {
                it[Tickets.status] = TicketStatus.COMPLETED.name
                it[Tickets.updatedAt] = LocalDateTime.now()
                it[Tickets.completedAt] = LocalDateTime.now()
                it[Tickets.comments] = comments
            }

            getTicketById(ticketId)
        }
    }

    fun getTicketById(ticketId: String): TicketDto? {
        return transaction {
            val ticketUuid = try {
                UUID.fromString(ticketId)
            } catch (e: Exception) {
                return@transaction null
            }

            val ticket = Tickets.select { Tickets.id eq ticketUuid }.singleOrNull()
                ?: return@transaction null

            val creator = Users.select { Users.id eq ticket[Tickets.creatorId] }.singleOrNull()
                ?: return@transaction null // Заявка с удалённым создателем
            val assignee = ticket[Tickets.assigneeId]?.let {
                Users.select { Users.id eq it }.singleOrNull()
            }

            val assignedBy = ticket[Tickets.assignedById]?.let {
                Users.select { Users.id eq it }.singleOrNull()
            }

            TicketDto(
                id = ticket[Tickets.id].value.toString(),
                title = ticket[Tickets.title],
                description = ticket[Tickets.description],
                priority = TicketPriority.valueOf(ticket[Tickets.priority]),
                status = TicketStatus.valueOf(ticket[Tickets.status]),
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
                assignedBy = assignedBy?.let {
                    UserSummaryDto(
                        id = it[Users.id].value.toString(),
                        name = it[Users.name],
                        role = UserRole.valueOf(it[Users.role])
                    )
                },
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
                    val techUuid = try {
                        UUID.fromString(userId)
                    } catch (e: Exception) {
                        return@transaction emptyList()
                    }
                    Tickets.select {
                        (Tickets.status eq TicketStatus.NEW.name) or
                        (Tickets.status eq TicketStatus.ASSIGNED.name) or
                        (Tickets.assigneeId eq techUuid)
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

            query.mapNotNull { row ->
                // Если указан период, фильтруем по дате завершения
                if (periodStart != null && row[Tickets.completedAt] != null) {
                    val completedAt = row[Tickets.completedAt]!!
                    if (completedAt.isBefore(periodStart)) {
                        return@mapNotNull null
                    }
                }
                
                val creator = Users.select { Users.id eq row[Tickets.creatorId] }.singleOrNull()
                    ?: return@mapNotNull null // Пропускаем заявки с удалёнными создателями
                
                val assignee = row[Tickets.assigneeId]?.let {
                    Users.select { Users.id eq it }.singleOrNull()
                }

                val assignedBy = row[Tickets.assignedById]?.let {
                    Users.select { Users.id eq it }.singleOrNull()
                }

                TicketDto(
                    id = row[Tickets.id].value.toString(),
                    title = row[Tickets.title],
                    description = row[Tickets.description],
                    priority = TicketPriority.valueOf(row[Tickets.priority]),
                    status = TicketStatus.valueOf(row[Tickets.status]),
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
                    assignedBy = assignedBy?.let {
                        UserSummaryDto(
                            id = it[Users.id].value.toString(),
                            name = it[Users.name],
                            role = UserRole.valueOf(it[Users.role])
                        )
                    },
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

