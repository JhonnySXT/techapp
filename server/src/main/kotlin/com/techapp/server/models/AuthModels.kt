package com.techapp.server.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String, // Фамилия и инициалы, например: "Иванов В.В."
    val password: String
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val user: UserSummaryDto
)

