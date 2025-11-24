package com.techapp.server.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TicketDto(
    val id: String,
    val title: String,
    val description: String,
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val status: TicketStatus = TicketStatus.NEW,
    val creator: UserSummaryDto,
    val assignee: UserSummaryDto?,
    val assignedBy: UserSummaryDto? = null,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond,
    val completedAt: Long? = null,
    val deadlineAt: Long? = null,
    val estimatedCompletionTime: Long? = null,
    val comments: String? = null,
    val photos: List<String> = emptyList() // Список путей к фотографиям
)

@Serializable
data class CompleteTicketRequest(
    val comments: String? = null
)

@Serializable
data class AcceptTicketRequest(
    val estimatedCompletionTime: Long? = null
)

@Serializable
data class AssignTicketRequest(
    val technicianId: String
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val name: String,
    val role: UserRole = UserRole.TECHNICIAN,
    val lastSeen: Long? = null,
    val isOnline: Boolean = false
)

@Serializable
enum class TicketPriority { LOW, MEDIUM, HIGH, CRITICAL }

@Serializable
enum class TicketStatus { NEW, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED }

@Serializable
enum class UserRole { ADMIN, MANAGER, DIRECTOR, TECHNICIAN }

