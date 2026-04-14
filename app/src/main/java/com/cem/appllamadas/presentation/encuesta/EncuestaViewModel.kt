package com.cem.appllamadas.presentation.encuesta

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cem.appllamadas.domain.model.Encuesta
import com.cem.appllamadas.domain.model.EstadoContacto
import com.cem.appllamadas.domain.model.EstadoEncuesta
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.domain.repository.EncuestaRepository
import com.cem.appllamadas.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EncuestaViewModel @Inject constructor(
    private val encuestaRepository: EncuestaRepository,
    private val contactoRepository: ContactoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun registrarEstadoEncuesta(contactoId: String, url: String, estado: EstadoEncuesta, onDone: () -> Unit) {
        viewModelScope.launch {
            val encuesta = Encuesta(
                id = UUID.randomUUID().toString(),
                contactoId = contactoId,
                url = url,
                estado = estado,
                fecha = System.currentTimeMillis()
            )
            encuestaRepository.guardarEncuesta(encuesta)

            // Actualizar estado del contacto a CONTACTADO si se hizo la encuesta
            val contacto = contactoRepository.obtenerContacto(contactoId)
            if (contacto != null) {
                val nuevoEstado = when (estado) {
                    EstadoEncuesta.COMPLETA, EstadoEncuesta.INCOMPLETA -> EstadoContacto.CONTACTADO
                    else -> contacto.estado
                }
                
                if (nuevoEstado != contacto.estado) {
                    contactoRepository.actualizarContacto(contacto.copy(estado = nuevoEstado))
                }
            }

            // Sincronizar inmediatamente al servidor para reflejar en el dashboard
            SyncWorker.dispatchImmediate(context)

            // Lanza el onDone para volver atrás
            onDone()
        }
    }
}
