package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.TipificacionDao
import com.cem.appllamadas.data.local.entity.TipificacionEntity
import com.cem.appllamadas.data.remote.ApiService
import com.cem.appllamadas.domain.model.Tipificacion
import com.cem.appllamadas.domain.repository.TipificacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TipificacionRepositoryImpl @Inject constructor(
    private val dao: TipificacionDao,
    private val apiService: ApiService
) : TipificacionRepository {

    override fun getAll(): Flow<List<Tipificacion>> {
        return dao.getAll().map { list ->
            list.map { Tipificacion(it.id, it.nombre, it.resultado, it.cierraCaso) }
        }
    }

    override suspend fun syncTipificaciones() {
        try {
            val response = apiService.getTipificaciones()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = dtos.map {
                    TipificacionEntity(it.id, it.nombre, it.resultado, it.cierraCaso)
                }
                dao.insertAll(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
