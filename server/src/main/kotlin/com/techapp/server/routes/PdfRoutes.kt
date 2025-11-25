package com.techapp.server.routes

import com.techapp.server.models.UserRole
import com.techapp.server.plugins.requireAnyRole
import com.techapp.server.services.AuthService
import com.techapp.server.services.TicketService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Route.pdfRoutes() {
    route("/tickets") {
        authenticate("jwt") {
            get("/export/pdf") {
                try {
                    call.requireAnyRole(UserRole.ADMIN, UserRole.MANAGER, UserRole.DIRECTOR)
                    
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    val role = principal?.payload?.getClaim("role")?.asString()?.let { UserRole.valueOf(it) }
                    
                    // Получаем период из параметров запроса
                    val period = call.request.queryParameters["period"] ?: "day"
                    
                    val tickets = TicketService.listTickets(userId, role, period)
                    
                    val pdfBytes = generatePdf(tickets, period)
                    
                    val periodName = when (period) {
                        "hour" -> "час"
                        "day" -> "день"
                        "month" -> "месяц"
                        else -> "период"
                    }
                    
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"отчет_за_${periodName}_${System.currentTimeMillis()}.pdf\""
                    )
                    call.response.header("Content-Type", "application/pdf")
                    call.respond(pdfBytes)
                } catch (e: Exception) {
                    val logger = org.slf4j.LoggerFactory.getLogger(PdfRoutes::class.java)
                    logger.error("Ошибка при генерации PDF", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Ошибка генерации PDF: ${e.message ?: "Неизвестная ошибка"}")
                    )
                }
            }
        }
    }
}

fun generatePdf(tickets: List<com.techapp.server.models.TicketDto>, period: String): ByteArray {
    // Функция для транслитерации кириллицы в латиницу (для совместимости с PDFBox)
    fun transliterate(text: String): String {
        val map = mapOf(
            'А' to "A", 'а' to "a", 'Б' to "B", 'б' to "b", 'В' to "V", 'в' to "v",
            'Г' to "G", 'г' to "g", 'Д' to "D", 'д' to "d", 'Е' to "E", 'е' to "e",
            'Ё' to "Yo", 'ё' to "yo", 'Ж' to "Zh", 'ж' to "zh", 'З' to "Z", 'з' to "z",
            'И' to "I", 'и' to "i", 'Й' to "Y", 'й' to "y", 'К' to "K", 'к' to "k",
            'Л' to "L", 'л' to "l", 'М' to "M", 'м' to "m", 'Н' to "N", 'н' to "n",
            'О' to "O", 'о' to "o", 'П' to "P", 'п' to "p", 'Р' to "R", 'р' to "r",
            'С' to "S", 'с' to "s", 'Т' to "T", 'т' to "t", 'У' to "U", 'у' to "u",
            'Ф' to "F", 'ф' to "f", 'Х' to "Kh", 'х' to "kh", 'Ц' to "Ts", 'ц' to "ts",
            'Ч' to "Ch", 'ч' to "ch", 'Ш' to "Sh", 'ш' to "sh", 'Щ' to "Shch", 'щ' to "shch",
            'Ъ' to "", 'ъ' to "", 'Ы' to "Y", 'ы' to "y", 'Ь' to "", 'ь' to "",
            'Э' to "E", 'э' to "e", 'Ю' to "Yu", 'ю' to "yu", 'Я' to "Ya", 'я' to "ya"
        )
        return text.map { map[it] ?: it }.joinToString("")
    }
    
    val document = PDDocument()
    val page = PDPage(PDRectangle.A4)
    document.addPage(page)
    
    var contentStream = PDPageContentStream(document, page)
    val margin = 50f
    val pageWidth = page.mediaBox.width - 2 * margin
    var yPosition = page.mediaBox.height - margin
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    
    // Фильтруем только выполненные заявки
    val completedTickets = tickets.filter { 
        it.status == com.techapp.server.models.TicketStatus.COMPLETED && 
        it.completedAt != null 
    }
    
    // Заголовок
    val periodName = when (period) {
        "hour" -> "za posledniy chas"
        "day" -> "za segodnya"
        "month" -> "za tekushiy mesyats"
        else -> ""
    }
    
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText("Otchet o vypolnennyh zayavkah $periodName")
    contentStream.endText()
    yPosition -= 30f
    
    // Статистика по техникам
    val technicianStats = completedTickets
        .filter { it.assignee != null }
        .groupBy { it.assignee!!.name }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    
    if (technicianStats.isNotEmpty()) {
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
        contentStream.newLineAtOffset(margin, yPosition)
        contentStream.showText("Statistika po tehnikam:")
        contentStream.endText()
        yPosition -= 20f
        
        technicianStats.forEach { (name, count) ->
            if (yPosition < 100f) {
                contentStream.close()
                val newPage = PDPage(PDRectangle.A4)
                document.addPage(newPage)
                contentStream = PDPageContentStream(document, newPage)
                yPosition = page.mediaBox.height - margin
            }
            
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(margin + 10f, yPosition)
            val techName = transliterate(name).take(40) + if (name.length > 40) "..." else ""
            contentStream.showText("$techName: $count zayavok")
            contentStream.endText()
            yPosition -= 15f
        }
        
        yPosition -= 15f
    }
    
    // Заявки
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText("Detali vypolnennyh zayavok:")
    contentStream.endText()
    yPosition -= 25f
    
    completedTickets.forEachIndexed { index, ticket ->
        if (yPosition < 100f) {
            contentStream.close()
            val newPage = PDPage(PDRectangle.A4)
            document.addPage(newPage)
            contentStream = PDPageContentStream(document, newPage)
            yPosition = page.mediaBox.height - margin
        }
        
        // Номер заявки
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
        contentStream.newLineAtOffset(margin, yPosition)
        val title = transliterate(ticket.title).take(50) + if (ticket.title.length > 50) "..." else ""
        contentStream.showText("Zayavka #${index + 1}: $title")
        contentStream.endText()
        yPosition -= 20f
        
        // Описание
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(margin + 10f, yPosition)
        val description = transliterate(ticket.description).take(100) + if (ticket.description.length > 100) "..." else ""
        contentStream.showText("Opisanie: $description")
        contentStream.endText()
        yPosition -= 15f
        
        // Приоритет
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(margin + 10f, yPosition)
        contentStream.showText("Prioritet: ${ticket.priority}")
        contentStream.endText()
        yPosition -= 15f
        
        // Создатель и исполнитель
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(margin + 10f, yPosition)
        val creatorName = transliterate(ticket.creator.name).take(30)
        val assigneeName = ticket.assignee?.let { transliterate(it.name).take(30) } ?: "Ne naznachen"
        contentStream.showText("Sozdatel: $creatorName, Ispolnitel: $assigneeName")
        contentStream.endText()
        yPosition -= 15f
        
        // Дата создания
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(margin + 10f, yPosition)
        val createdDate = Instant.ofEpochSecond(ticket.createdAt)
        contentStream.showText("Sozdano: ${dateFormatter.format(createdDate)}")
        contentStream.endText()
        yPosition -= 15f
        
        // Дата завершения
        if (ticket.completedAt != null) {
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(margin + 10f, yPosition)
            val completedDate = Instant.ofEpochSecond(ticket.completedAt)
            contentStream.showText("Zaversheno: ${dateFormatter.format(completedDate)}")
            contentStream.endText()
            yPosition -= 15f
        }
        
        // Комментарии
        if (!ticket.comments.isNullOrBlank()) {
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(margin + 10f, yPosition)
            val comments = transliterate(ticket.comments).take(80) + if (ticket.comments.length > 80) "..." else ""
            contentStream.showText("Kommentarii: $comments")
            contentStream.endText()
            yPosition -= 15f
        }
        
        yPosition -= 15f
    }
    
    // Итоговая статистика
    if (yPosition < 150f) {
        contentStream.close()
        val newPage = PDPage(PDRectangle.A4)
        document.addPage(newPage)
        contentStream = PDPageContentStream(document, newPage)
        yPosition = page.mediaBox.height - margin
    }
    
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText("Itogovaya statistika:")
    contentStream.endText()
    yPosition -= 20f
    
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA, 10f)
    contentStream.newLineAtOffset(margin + 10f, yPosition)
    contentStream.showText("Vsego vypolneno zayavok: ${completedTickets.size}")
    contentStream.endText()
    yPosition -= 20f
    
    technicianStats.forEach { (name, count) ->
        if (yPosition < 50f) {
            contentStream.close()
            val newPage = PDPage(PDRectangle.A4)
            document.addPage(newPage)
            contentStream = PDPageContentStream(document, newPage)
            yPosition = page.mediaBox.height - margin
        }
        
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(margin + 10f, yPosition)
        val techName = transliterate(name).take(40) + if (name.length > 40) "..." else ""
        contentStream.showText("$techName: $count zayavok")
        contentStream.endText()
        yPosition -= 15f
    }
    
    contentStream.close()
    
    val outputStream = ByteArrayOutputStream()
    document.save(outputStream)
    document.close()
    
    return outputStream.toByteArray()
}

