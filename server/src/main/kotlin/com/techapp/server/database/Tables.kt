package com.techapp.server.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255)
    val role = varchar("role", 50)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val lastSeen = datetime("last_seen").nullable()
}

object Tickets : UUIDTable("tickets") {
    val title = varchar("title", 500)
    val description = text("description")
    val priority = varchar("priority", 50)
    val status = varchar("status", 50)
    val creatorId = reference("creator_id", Users.id)
    val assigneeId = reference("assignee_id", Users.id).nullable()
    val assignedById = reference("assigned_by_id", Users.id).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val completedAt = datetime("completed_at").nullable()
    val deadlineAt = datetime("deadline_at").nullable()
    val estimatedCompletionTime = datetime("estimated_completion_time").nullable()
    val comments = text("comments").nullable()
    val photos = text("photos").nullable() // JSON массив путей к фотографиям
}

