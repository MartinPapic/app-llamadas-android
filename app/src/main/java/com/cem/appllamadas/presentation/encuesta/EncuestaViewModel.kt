package com.cem.appllamadas.presentation.encuesta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cem.appllamadas.domain.model.Encuesta
import com.cem.appllamadas.domain.model.EstadoEncuesta
import com.cem.appllamadas.domain.repository.EncuestaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EncuestaViewModel @Inject constructor(
    private val encuestaRepository: EncuestaRepository
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
            // Lanza el onDone para volver atrás
            onDone()
        }
    }
}
