package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.ContactoDao
import com.cem.appllamadas.data.local.dao.LlamadaDao
import com.cem.appllamadas.data.local.entity.LlamadaEntity
import com.cem.appllamadas.data.remote.ApiService
import com.cem.appllamadas.data.remote.ContactoDto
import com.cem.appllamadas.data.remote.LlamadaDto
import com.cem.appllamadas.data.remote.SyncPayload
import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.model.Llamada
import com.cem.appllamadas.domain.model.ResultadoLlamada
import com.cem.appllamadas.domain.repository.LlamadaRepository

class LlamadaRepositoryImpl(
    private val llamadaDao: LlamadaDao,
    private val contactoDao: ContactoDao,
    private val apiService: ApiService
) : LlamadaRepository {

    // ─── Escritura local (offline-first) ──────────────────────────────────────

    override suspend fun registrarLlamada(llamada: Llamada) {
        llamadaDao.insertLlamada(llamada.toEntity())
    }

    // ─── Sincronización batch con el backend ──────────────────────────────────

    override suspend fun syncLlamadasPendientes() {
        val pendientes = llamadaDao.getLlamadasPendientesSync()
        if (pendientes.isEmpty()) return

        val llamadasDto = pendientes.map { it.toDto() }

        // Contactos asociados a las llamadas pendientes
        val contactosDto = pendientes
            .mapNotNull { contactoDao.getContactoById(it.contactoId) }
            .distinctBy { it.id }
            .map { entity ->
                ContactoDto(
                    id           = entity.id,
                    nombre       = entity.nombre,
                    telefono     = entity.telefono,
                    estado       = entity.estado.name.lowercase(),
                    intentos     = entity.intentos,
                    fechaCreacion = entity.fechaCreacion
                )
            }

        try {
            val response = apiService.syncData(
                SyncPayload(
                    llamadas              = llamadasDto,
                    contactosActualizados = contactosDto
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                // Marcar como sincronizadas en Room
                pendientes.forEach { llamadaDao.marcarSincronizada(it.id) }
            }
        } catch (e: Exception) {
            // Sin conexión → se reintentará en el próximo ciclo del Worker
            throw e
        }
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private fun LlamadaEntity.toDto() = LlamadaDto(
        id           = id,
        contactoId   = contactoId,
        usuarioId    = usuarioId,
        fechaInicio  = fechaInicio,
        fechaFin     = fechaFin,
        duracion     = duracion,
        resultado    = resultado?.name,
        tipificacion = tipificacion,
        observacion  = observacion
    )

    private fun LlamadaEntity.toDomain() = Llamada(
        id           = id,
        contactoId   = contactoId,
        usuarioId    = usuarioId,
        fechaInicio  = fechaInicio,
        fechaFin     = fechaFin,
        duracion     = duracion,
        resultado    = resultado,
        tipificacion = tipificacion,
        observacion  = observacion,
        pendienteSync = pendienteSync
    )

    private fun Llamada.toEntity() = LlamadaEntity(
        id           = id,
        contactoId   = contactoId,
        usuarioId    = usuarioId,
        fechaInicio  = fechaInicio,
        fechaFin     = fechaFin,
        duracion     = duracion,
        resultado    = resultado,
        tipificacion = tipificacion,
        observacion  = observacion,
        pendienteSync = pendienteSync
    )
}
