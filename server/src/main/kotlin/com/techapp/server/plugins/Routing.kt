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
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.routing.routing
import io.ktor.server.routing.route

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
    }
}

