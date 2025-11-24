package com.techapp.server.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Database {
    private val dbPath = File("data/techapp.db").absolutePath
    
    fun init() {
        try {
            // Создаём директорию если её нет
            val dataDir = File("data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            
            org.jetbrains.exposed.sql.Database.connect(
                "jdbc:sqlite:$dbPath",
                driver = "org.sqlite.JDBC"
            )
            
            transaction {
                SchemaUtils.createMissingTablesAndColumns(Users, Tickets)
            }
            
            // Инициализируем пользователей (удаляем старых и создаём новых)
            transaction {
                InitUsers.initUsers()
            }
        } catch (e: Exception) {
            throw RuntimeException("Ошибка инициализации базы данных: ${e.message}", e)
        }
    }
}

