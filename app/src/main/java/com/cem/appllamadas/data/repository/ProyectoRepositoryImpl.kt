package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.ProyectoDao
import com.cem.appllamadas.data.local.entity.ProyectoEntity
import com.cem.appllamadas.data.remote.ApiService
import com.cem.appllamadas.domain.repository.ProyectoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProyectoRepositoryImpl @Inject constructor(
    private val proyectoDao: ProyectoDao,
    private val apiService: ApiService
) : ProyectoRepository {

    override fun getAllProyectos(): Flow<List<ProyectoEntity>> = proyectoDao.getAllProyectos()

    override suspend fun syncProyectosDesdeServidor() {
        try {
            val response = apiService.getProyectosAgente()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = dtos.map { 
                    ProyectoEntity(it.id, it.nombre, it.instrumentoUrl, it.fechaCreacion) 
                }
                proyectoDao.insertProyectos(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun insertProyectos(proyectos: List<ProyectoEntity>) {
        proyectoDao.insertProyectos(proyectos)
    }
}
