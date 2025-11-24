package com.techapp.server.routes

import com.techapp.server.models.UserRole
import com.techapp.server.plugins.requireAnyRole
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.io.File
import java.util.UUID

fun Route.fileRoutes() {
    route("/files") {
        authenticate("jwt") {
            post("/upload") {
                call.requireAnyRole(UserRole.ADMIN, UserRole.MANAGER, UserRole.DIRECTOR)
                
                // Создаём директорию для файлов если её нет
                val uploadsDir = File("uploads")
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs()
                }
                
                val multipart = call.receiveMultipart()
                val uploadedFiles = mutableListOf<String>()
                
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        // Ограничиваем количество файлов до 5
                        if (uploadedFiles.size >= 5) {
                            part.dispose()
                            return@forEachPart
                        }
                        
                        // Проверяем тип файла (только изображения)
                        val contentType = part.contentType?.toString() ?: ""
                        if (!contentType.startsWith("image/")) {
                            part.dispose()
                            return@forEachPart
                        }
                        
                        // Генерируем уникальное имя файла
                        val fileExtension = part.originalFileName?.substringAfterLast('.', "") ?: "jpg"
                        val fileName = "${UUID.randomUUID()}.$fileExtension"
                        val file = File(uploadsDir, fileName)
                        
                        // Сохраняем файл
                        part.streamProvider().use { input ->
                            file.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Сохраняем путь к файлу (относительный от корня сервера)
                        uploadedFiles.add("uploads/$fileName")
                        part.dispose()
                    } else {
                        part.dispose()
                    }
                }
                
                call.respond(mapOf("files" to uploadedFiles))
            }
        }
    }
}

// Отдельный route для получения файлов (должен быть вне authenticate)
fun Route.fileDownloadRoutes() {
    route("/uploads") {
        get("/{path...}") {
            val pathParts = call.parameters.getAll("path") ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Путь не указан"))
                return@get
            }
            
            // Строим путь: если pathParts содержит "uploads", убираем его, иначе используем как есть
            val relativePath = if (pathParts.firstOrNull() == "uploads") {
                pathParts.drop(1).joinToString(File.separator)
            } else {
                pathParts.joinToString(File.separator)
            }
            
            // Строим полный путь к файлу в директории uploads
            val file = File("uploads", relativePath)
            
            if (!file.exists() || !file.isFile) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Файл не найден"))
                return@get
            }
            
            // Проверяем, что файл находится в директории uploads
            val uploadsDir = File("uploads").canonicalPath
            if (!file.canonicalPath.startsWith(uploadsDir)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Доступ запрещен"))
                return@get
            }
            
            // respondFile автоматически определяет Content-Type на основе расширения файла
            call.respondFile(file)
        }
    }
}

