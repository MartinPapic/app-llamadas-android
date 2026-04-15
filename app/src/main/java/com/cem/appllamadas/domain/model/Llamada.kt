package com.cem.appllamadas.domain.model

enum class ResultadoLlamada {
    CONTACTADO_EFECTIVO,
    CONTACTADO_NO_EFECTIVO,
    NO_CONTACTADO
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
    val motivo: String?,
    val observacion: String?,
    val pendienteSync: Boolean = true
)
