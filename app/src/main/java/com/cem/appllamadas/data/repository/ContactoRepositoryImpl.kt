package com.cem.appllamadas.data.repository

import com.cem.appllamadas.data.local.dao.ContactoDao
import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.data.local.entity.ContactoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactoRepositoryImpl(
    private val contactoDao: ContactoDao
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

    private fun ContactoEntity.toDomain() = Contacto(
        id = id,
        nombre = nombre,
        telefono = telefono,
        estado = estado,
        intentos = intentos,
        fechaCreacion = fechaCreacion
    )

    private fun Contacto.toEntity() = ContactoEntity(
        id = id,
        nombre = nombre,
        telefono = telefono,
        estado = estado,
        intentos = intentos,
        fechaCreacion = fechaCreacion
    )
}
