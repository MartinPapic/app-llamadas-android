package com.cem.appllamadas.domain.usecase

import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.repository.ContactoRepository
import java.util.Calendar

class ObtenerSiguienteContactoUseCase(
    private val contactoRepository: ContactoRepository
) {
    suspend operator fun invoke(): Contacto? {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayTimestamp = calendar.timeInMillis
        return contactoRepository.getSiguienteContacto(startOfDayTimestamp)
    }
}
