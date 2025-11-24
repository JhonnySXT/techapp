package com.techapp.server.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.directorySessionStorage
import java.io.File

data class SessionPrincipal(val userId: String) : java.io.Serializable

fun Application.configureSecurity() {
    val env = environment
    install(CORS) {
        allowSameOrigin = true
        allowCredentials = true
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader("X-Request-ID")
        anyHost()
    }

    install(Sessions) {
        val storageDirPath = env.config.propertyOrNull("ktor.storage.dir")?.getString()
            ?: "${env.rootPath}/sessions"
        val storageDir = File(storageDirPath).apply { mkdirs() }
        cookie<SessionPrincipal>(
            name = "techapp_session",
            storage = directorySessionStorage(storageDir)
        ) {
            cookie.extensions["SameSite"] = "None"
            cookie.httpOnly = true
            cookie.secure = false
        }
    }

}

