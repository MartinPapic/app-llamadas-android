package com.cem.appllamadas.domain.model

enum class RolUsuario {
    AGENTE, ADMIN
}

data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val passwordHash: String,
    val rol: RolUsuario
)
