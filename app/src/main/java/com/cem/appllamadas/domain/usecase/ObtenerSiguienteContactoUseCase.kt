package com.cem.appllamadas.domain.usecase

import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.repository.ContactoRepository

class ObtenerSiguienteContactoUseCase(
    private val contactoRepository: ContactoRepository
) {
    suspend operator fun invoke(): Contacto? {
        // Obtiene el siguiente contacto que no esté desistido ni contactado
        return contactoRepository.getSiguienteContacto()
    }
}
