package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.domain.model.Encuesta

interface EncuestaRepository {
    suspend fun guardarEncuesta(encuesta: Encuesta)
    suspend fun syncEncuestasPendientes(): Boolean
}
