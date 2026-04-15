package com.cem.appllamadas.domain.model

enum class EstadoContacto {
    PENDIENTE, EN_GESTION, CONTACTADO, DESISTIDO
}

data class Contacto(
    val id: String,
    val nombre: String,
    val telefono: String,
    val estado: EstadoContacto,
    val intentos: Int,
    val fechaCreacion: Long,
    val ultimaTipificacion: String? = null,
    val ultimaObservacion: String? = null
)
