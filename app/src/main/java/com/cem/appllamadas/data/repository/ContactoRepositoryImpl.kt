package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.ContactoDao
import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.data.local.entity.ContactoEntity
import com.cem.appllamadas.domain.model.EstadoContacto
import com.cem.appllamadas.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactoRepositoryImpl(
    private val contactoDao: ContactoDao,
    private val apiService: ApiService
) : ContactoRepository {

    override suspend fun getSiguienteContacto(): Contacto? {
        val entity = contactoDao.getSiguienteContacto()
        return entity?.toDomain()
    }

    override suspend fun actualizarContacto(contacto: Contacto) {
        contactoDao.updateContacto(contacto.toEntity())
    }

    override suspend fun obtenerContacto(id: String): Contacto? {
        return contactoDao.getContactoById(id)?.toDomain()
    }

    override fun getAllContactos(): Flow<List<Contacto>> =
        contactoDao.getAllContactos().map { list -> list.map { it.toDomain() } }

    override suspend fun syncContactosDesdeServidor() {
        try {
            // Obtenemos solo los pendientes o en_gestion (los activos)
            val response = apiService.getContactos("pendiente") 
            if (response.isSuccessful) {
                val remotos = response.body() ?: emptyList()
                val entidades = remotos.map { dto ->
                    ContactoEntity(
                        id = dto.id,
                        nombre = dto.nombre,
                        telefono = dto.telefono,
                        estado = EstadoContacto.valueOf(dto.estado.uppercase()),
                        intentos = dto.intentos,
                        fechaCreacion = dto.fechaCreacion
                    )
                }
                if (entidades.isNotEmpty()) {
                    contactoDao.insertContactos(entidades)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallo silencioso: si no hay red, la vista ya mostrará el caché
        }
    }

    override suspend fun lockContacto(id: String): Result<Unit> {
        return try {
            val response = apiService.lockContacto(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                if (response.code() == 409) {
                    Result.failure(Exception("CONCURRENCE_ERROR"))
                } else {
                    Result.failure(Exception("Error de red: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

        ultimaTipificacion = ultimaTipificacion,
        ultimaObservacion = ultimaObservacion,
        fechaUltimaGestion = fechaUltimaGestion
    )

    private fun Contacto.toEntity() = ContactoEntity(
        id = id,
        nombre = nombre,
        telefono = telefono,
        estado = estado,
        intentos = intentos,
        fechaCreacion = fechaCreacion,
        ultimaTipificacion = ultimaTipificacion,
        ultimaObservacion = ultimaObservacion,
        fechaUltimaGestion = fechaUltimaGestion
    )
}
