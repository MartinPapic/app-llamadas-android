package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.domain.model.Tipificacion
import kotlinx.coroutines.flow.Flow

interface TipificacionRepository {
    fun getAll(): Flow<List<Tipificacion>>
    suspend fun syncTipificaciones()
}
