package com.cem.appllamadas.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ─── DTOs ────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val rol: String,
    val nombre: String,
    val userId: String
)

// ─── Auth API ─────────────────────────────────────────────────────────────────

interface AuthApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
