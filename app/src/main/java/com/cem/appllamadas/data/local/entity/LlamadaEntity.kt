package com.cem.appllamadas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cem.appllamadas.domain.model.ResultadoLlamada

@Entity(tableName = "llamada")
data class LlamadaEntity(
    @PrimaryKey val id: String,
    val contactoId: String,
    val usuarioId: String,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val duracion: Int?,
    val resultado: ResultadoLlamada?,
    val tipificacion: String?,
    val motivo: String?,
    val observacion: String?,
    val proyectoId: String?,
    val pendienteSync: Boolean = true
)
