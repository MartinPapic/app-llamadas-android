package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.EncuestaDao
import com.cem.appllamadas.data.local.entity.EncuestaEntity
import com.cem.appllamadas.data.remote.ApiService
import com.cem.appllamadas.data.remote.EncuestaDto
import com.cem.appllamadas.data.remote.EncuestaSyncRequest
import com.cem.appllamadas.domain.model.Encuesta
import com.cem.appllamadas.domain.repository.EncuestaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncuestaRepositoryImpl(
    private val encuestaDao: EncuestaDao,
    private val apiService: ApiService
) : EncuestaRepository {

    override suspend fun guardarEncuesta(encuesta: Encuesta) {
        val entity = EncuestaEntity(
            id = encuesta.id,
            contactoId = encuesta.contactoId,
            url = encuesta.url,
            estado = encuesta.estado,
            fecha = encuesta.fecha,
            pendienteSync = true
        )
        encuestaDao.insertEncuesta(entity)
    }

    override suspend fun syncEncuestasPendientes(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pendientes = encuestaDao.getEncuestasNoSincronizadas()
            if (pendientes.isEmpty()) return@withContext true

            val dtos = pendientes.map {
                EncuestaDto(
                    id = it.id,
                    contactoId = it.contactoId,
                    url = it.url,
                    estado = it.estado.name,
                    fecha = it.fecha
                )
            }

            val request = EncuestaSyncRequest(dtos)
            val response = apiService.syncEncuestas(request)

            if (response.isSuccessful) {
                encuestaDao.marcarSincronizadas(pendientes.map { it.id })
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
