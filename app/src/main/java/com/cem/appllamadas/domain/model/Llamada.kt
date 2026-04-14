package com.cem.appllamadas.domain.model

enum class ResultadoLlamada {
    CONTESTA, NO_CONTESTA, OCUPADO, INVALIDO
}

data class Llamada(
    val id: String,
    val contactoId: String,
    val usuarioId: String,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val duracion: Int?, // Segundos
    val resultado: ResultadoLlamada?,
    val tipificacion: String?,
    val observacion: String?,
    val pendienteSync: Boolean = true
)
