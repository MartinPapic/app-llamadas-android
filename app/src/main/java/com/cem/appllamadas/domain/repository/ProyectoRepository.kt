package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.data.local.entity.ProyectoEntity
import kotlinx.coroutines.flow.Flow

interface ProyectoRepository {
    fun getAllProyectos(): Flow<List<ProyectoEntity>>
    suspend fun syncProyectosDesdeServidor()
    suspend fun insertProyectos(proyectos: List<ProyectoEntity>)
}
