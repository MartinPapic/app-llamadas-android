package com.cem.appllamadas.domain.repository

import com.cem.appllamadas.domain.model.Contacto
import kotlinx.coroutines.flow.Flow

interface ContactoRepository {
    suspend fun getSiguienteContacto(): Contacto?
    suspend fun actualizarContacto(contacto: Contacto)
    suspend fun obtenerContacto(id: String): Contacto?
    fun getAllContactos(): Flow<List<Contacto>>
    suspend fun syncContactosDesdeServidor()
    suspend fun lockContacto(id: String): Result<Unit>
}
