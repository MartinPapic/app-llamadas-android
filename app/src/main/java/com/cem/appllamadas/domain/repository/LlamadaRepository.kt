package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.domain.model.Llamada

interface LlamadaRepository {
    suspend fun registrarLlamada(llamada: Llamada)
    suspend fun syncLlamadasPendientes() // Simulará el envío al backend
}
