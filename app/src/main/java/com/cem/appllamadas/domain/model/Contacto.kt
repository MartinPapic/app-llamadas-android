package com.cem.appllamadas.domain.model

enum class EstadoContacto {
    PENDIENTE, EN_GESTION, CONTACTADO, DESISTIDO, CERRADO, CERRADO_POR_INTENTOS
}

data class Contacto(
    val id: String,
    val nombre: String,
    val telefono: String,
    val estado: EstadoContacto,
    val intentos: Int,
    val fechaCreacion: Long,
    val ultimaTipificacion: String? = null,
    val ultimaObservacion: String? = null,
    val fechaUltimaGestion: Long? = null,
    val proyectoId: String? = null,
    val listaId: String? = null,
    val referenciaId: String? = null,
    val intentosValidos: Int = 0
)
