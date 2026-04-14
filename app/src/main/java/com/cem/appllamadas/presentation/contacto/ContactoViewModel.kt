package com.cem.appllamadas.presentation.contacto

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cem.appllamadas.call.CallState
import com.cem.appllamadas.call.CallStateManager
import com.cem.appllamadas.data.local.SessionManager
import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.model.Llamada
import com.cem.appllamadas.domain.model.ResultadoLlamada
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.domain.usecase.ObtenerSiguienteContactoUseCase
import com.cem.appllamadas.domain.usecase.RegistrarLlamadaUseCase
import com.cem.appllamadas.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Estado del formulario post-llamada
data class PostCallState(
    val resultado: ResultadoLlamada?,   // null = agente todavía no seleccionó
    val duracion: Int,
    val fechaInicio: Long,
    val fechaFin: Long
)

@HiltViewModel
class ContactoViewModel @Inject constructor(
    private val obtenerSiguienteContactoUseCase: ObtenerSiguienteContactoUseCase,
    private val registrarLlamadaUseCase: RegistrarLlamadaUseCase,
    private val contactoRepository: ContactoRepository,
    private val sessionManager: SessionManager,
    val callStateManager: CallStateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ─── Lista completa de contactos (para el listado) ────────────────────────
    val todosLosContactos: StateFlow<List<Contacto>> = contactoRepository
        .getAllContactos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Contacto seleccionado actualmente ───────────────────────────────────
    private val _contactoActual = MutableStateFlow<Contacto?>(null)
    val contactoActual: StateFlow<Contacto?> = _contactoActual.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ─── Formulario post-llamada ──────────────────────────────────────────────
    private val _postCallState = MutableStateFlow<PostCallState?>(null)
    val postCallState: StateFlow<PostCallState?> = _postCallState.asStateFlow()

    // ─── Navegación: mostrar listado o detalle ────────────────────────────────
    private val _mostrarListado = MutableStateFlow(true)
    val mostrarListado: StateFlow<Boolean> = _mostrarListado.asStateFlow()

    init {
        observeCallState()
    }

    private fun observeCallState() {
        viewModelScope.launch {
            callStateManager.callState.collect { state ->
                if (state is CallState.Ended) {
                    val result = state.result
                    _postCallState.value = PostCallState(
                        resultado   = null,   // El agente debe seleccionar manualmente
                        duracion    = result.duracion,
                        fechaInicio = result.fechaInicio,
                        fechaFin    = result.fechaFin
                    )
                }
            }
        }
    }

    /** Selecciona un contacto del listado para ver su detalle */
    fun seleccionarContacto(contacto: Contacto) {
        _contactoActual.value = contacto
        _mostrarListado.value = false
    }

    /** Vuelve al listado desde el detalle */
    fun volverAlListado() {
        _mostrarListado.value = true
        _contactoActual.value = null
        callStateManager.resetState()
        _postCallState.value = null
    }

    fun iniciarLlamada() {
        callStateManager.startTracking()
    }

    /**
     * Guarda la llamada con resultado seleccionado manualmente por el agente.
     * Resultado es obligatorio (RF-A7).
     */
    fun confirmarRegistro(
        resultadoSeleccionado: ResultadoLlamada,
        tipificacion: String,
        observacion: String
    ) {
        val post     = _postCallState.value ?: return
        val contacto = _contactoActual.value ?: return
        val userId   = sessionManager.getUserId() ?: "desconocido"

        viewModelScope.launch {
            val llamada = Llamada(
                id           = UUID.randomUUID().toString(),
                contactoId   = contacto.id,
                usuarioId    = userId,
                fechaInicio  = post.fechaInicio,
                fechaFin     = post.fechaFin,
                duracion     = post.duracion,
                resultado    = resultadoSeleccionado,
                tipificacion = tipificacion,
                observacion  = observacion.ifBlank { null }
            )
            registrarLlamadaUseCase(llamada, contacto)
            _postCallState.value = null
            callStateManager.resetState()
            // Sync inmediato si hay red
            SyncWorker.dispatchImmediate(context)
            // Volver al listado después de registrar
            _mostrarListado.value = true
            _contactoActual.value = null
        }
    }

    /** Registra una llamada manual sin haber usado el marcador */
    fun registrarLlamadaManual(
        resultadoSeleccionado: ResultadoLlamada,
        tipificacion: String,
        observacion: String
    ) {
        val contacto = _contactoActual.value ?: return
        val userId   = sessionManager.getUserId() ?: "desconocido"
        val ahora    = System.currentTimeMillis()

        viewModelScope.launch {
            val llamada = Llamada(
                id           = UUID.randomUUID().toString(),
                contactoId   = contacto.id,
                usuarioId    = userId,
                fechaInicio  = ahora,
                fechaFin     = ahora,
                duracion     = 0,
                resultado    = resultadoSeleccionado,
                tipificacion = tipificacion,
                observacion  = observacion.ifBlank { null }
            )
            registrarLlamadaUseCase(llamada, contacto)
            // Sync inmediato si hay red
            SyncWorker.dispatchImmediate(context)
            _mostrarListado.value = true
            _contactoActual.value = null
        }
    }
}
