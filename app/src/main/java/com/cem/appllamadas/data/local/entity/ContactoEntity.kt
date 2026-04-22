package com.cem.appllamadas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cem.appllamadas.domain.model.EstadoContacto

@Entity(tableName = "contacto")
data class ContactoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val telefono: String,
    val estado: EstadoContacto,
    val intentos: Int,
    val fechaCreacion: Long,
    val ultimaTipificacion: String? = null,
    val ultimaObservacion: String? = null,
    val proyectoId: String? = null
)
