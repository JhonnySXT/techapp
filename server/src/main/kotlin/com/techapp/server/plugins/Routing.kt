package com.techapp.server.plugins

import com.techapp.server.routes.authRoutes
import com.techapp.server.routes.fileDownloadRoutes
import com.techapp.server.routes.fileRoutes
import com.techapp.server.routes.healthRoutes
import com.techapp.server.routes.pdfRoutes
import com.techapp.server.routes.ticketRoutes
import com.techapp.server.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.http.content.resources
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.routing.routing
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.response.respondFile
import io.ktor.server.application.call
import java.io.File

fun Application.configureRouting() {
    install(AutoHeadResponse)

    routing {
        healthRoutes()
        route("/api/v1") {
            authRoutes()
            ticketRoutes()
            userRoutes()
            pdfRoutes()
            fileRoutes()
        }
        // Endpoint для получения файлов (вне /api/v1 для простоты)
        fileDownloadRoutes()
        
        // Статические файлы для фронтенда (PWA)
        // Ищем файлы в рабочей директории приложения (/app/)
        get("/{path...}") {
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val fileName = pathParts.lastOrNull() ?: "test-app.html"
            
            // Список статических файлов, которые нужно отдавать
            val staticFiles = listOf(
                "test-app.html", "test-login.html", 
                "config.js", "manifest.json", "sw.js",
                "icon-192.png", "icon-512.png"
            )
            
            // Определяем рабочую директорию (где запущен JAR)
            val workingDir = File(System.getProperty("user.dir") ?: ".")
            
            if (staticFiles.contains(fileName)) {
                // Пробуем найти файл в рабочей директории
                val file = File(workingDir, fileName)
                if (file.exists() && file.isFile) {
                    call.respondFile(file)
                    return@get
                }
                
                // Пробуем найти в resources/static
                val resourceFile = javaClass.classLoader.getResource("static/$fileName")
                if (resourceFile != null) {
                    call.respondFile(File(resourceFile.toURI()))
                    return@get
                }
            }
            
            // Если файл не найден и это корневой путь, отдаем test-app.html
            if (pathParts.isEmpty() || (pathParts.size == 1 && pathParts[0].isEmpty())) {
                val defaultFile = File(workingDir, "test-app.html")
                if (defaultFile.exists() && defaultFile.isFile) {
                    call.respondFile(defaultFile)
                    return@get
                }
                
                val resourceDefault = javaClass.classLoader.getResource("static/test-app.html")
                if (resourceDefault != null) {
                    call.respondFile(File(resourceDefault.toURI()))
                }
            }
        }
    }
}

