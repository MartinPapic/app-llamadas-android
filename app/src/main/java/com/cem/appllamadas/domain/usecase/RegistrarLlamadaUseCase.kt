package com.cem.appllamadas.domain.usecase

import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.model.EstadoContacto
import com.cem.appllamadas.domain.model.Llamada
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.domain.repository.LlamadaRepository

class RegistrarLlamadaUseCase(
    private val llamadaRepository: LlamadaRepository,
    private val contactoRepository: ContactoRepository
) {
    suspend operator fun invoke(llamada: Llamada, contacto: Contacto, cierraCaso: Boolean = false) {
        // Guardar llamada en base de datos local
        llamadaRepository.registrarLlamada(llamada)

        // Evaluar estado del contacto y los intentos
        val nuevosIntentos = contacto.intentos + 1
        val nuevosIntentosValidos = if (llamada.intentoValido) (contacto.intentosValidos + 1) else contacto.intentosValidos

        val nuevoEstado = when {
            llamada.resultado == com.cem.appllamadas.domain.model.ResultadoLlamada.CONTACTADO_EFECTIVO -> EstadoContacto.CONTACTADO
            cierraCaso -> EstadoContacto.DESISTIDO
            nuevosIntentosValidos >= 5 -> EstadoContacto.CERRADO_POR_INTENTOS
            else -> EstadoContacto.EN_GESTION
        }

        val contactoActualizado = contacto.copy(
            intentos = nuevosIntentos,
            intentosValidos = nuevosIntentosValidos,
            estado = nuevoEstado,
            ultimaTipificacion = llamada.tipificacion,
            ultimaObservacion = llamada.observacion,
            fechaUltimaGestion = llamada.fechaFin ?: System.currentTimeMillis()
        )
        contactoRepository.actualizarContacto(contactoActualizado)
    }
}
