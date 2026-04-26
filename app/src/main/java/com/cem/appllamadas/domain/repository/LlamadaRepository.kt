package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.domain.model.Llamada
import kotlinx.coroutines.flow.Flow

interface LlamadaRepository {
    suspend fun registrarLlamada(llamada: Llamada)
    suspend fun getLlamadasPendientesSync(): List<Llamada>
    suspend fun marcarComoSincronizada(id: String)
    fun getHistorialByContacto(contactoId: String): Flow<List<Llamada>>
}
