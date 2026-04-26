package com.cem.appllamadas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipificacion")
data class TipificacionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val resultado: String,
    val cierraCaso: Boolean
)
