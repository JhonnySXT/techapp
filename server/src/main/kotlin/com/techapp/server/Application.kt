package com.techapp.server

import com.techapp.server.database.Database
import com.techapp.server.plugins.configureAuth
import com.techapp.server.plugins.configureMonitoring
import com.techapp.server.plugins.configureRouting
import com.techapp.server.plugins.configureSecurity
import com.techapp.server.plugins.configureSerialization
import com.techapp.server.plugins.configureWebSocket
import io.ktor.server.application.Application
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        port = (System.getenv("PORT") ?: "8081").toInt(),
        host = System.getenv("HOST") ?: "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    environment.monitor.subscribe(ApplicationStarted) {
        try {
            Database.init()
            log.info("База данных инициализирована успешно")
        } catch (e: Exception) {
            log.error("Ошибка инициализации базы данных", e)
            throw e
        }
        log.info("Загородный клуб 'Дача' - сервер запущен.")
    }
    configureMonitoring()
    configureSerialization()
    configureSecurity()
    configureAuth()
    configureWebSocket()
    configureRouting()
    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Загородный клуб 'Дача' - сервер останавливается...")
    }
}

