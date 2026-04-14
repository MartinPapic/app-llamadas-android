package com.cem.appllamadas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cem.appllamadas.domain.model.EstadoEncuesta

@Entity(tableName = "encuesta")
data class EncuestaEntity(
    @PrimaryKey
    val id: String,
    val contactoId: String,
    val url: String,
    val estado: EstadoEncuesta,
    val fecha: Long,
    val pendienteSync: Boolean = true
)
