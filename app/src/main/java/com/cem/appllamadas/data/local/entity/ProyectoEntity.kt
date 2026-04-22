package com.cem.appllamadas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proyecto")
data class ProyectoEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val instrumentoUrl: String,
    val fechaCreacion: Long
)
