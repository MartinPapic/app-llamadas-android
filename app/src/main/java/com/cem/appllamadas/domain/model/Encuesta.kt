package com.cem.appllamadas.domain.model

enum class EstadoEncuesta {
    COMPLETA, INCOMPLETA, NO_REALIZADA
}

data class Encuesta(
    val id: String,
    val contactoId: String,
    val url: String,
    val estado: EstadoEncuesta,
    val fecha: Long
)
