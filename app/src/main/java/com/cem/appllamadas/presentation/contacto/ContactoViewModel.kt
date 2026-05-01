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
import com.cem.appllamadas.domain.repository.TipificacionRepository
import com.cem.appllamadas.domain.model.Tipificacion
import com.cem.appllamadas.domain.usecase.ObtenerSiguienteContactoUseCase
import com.cem.appllamadas.domain.usecase.RegistrarLlamadaUseCase
import com.cem.appllamadas.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val tipificacionRepository: TipificacionRepository,
    private val proyectoRepository: com.cem.appllamadas.domain.repository.ProyectoRepository,
    private val llamadaRepository: com.cem.appllamadas.domain.repository.LlamadaRepository,
    private val sessionManager: SessionManager,
    val callStateManager: CallStateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ─── Proyectos asignados ──────────────────────────────────────────────────
    val proyectos: StateFlow<List<com.cem.appllamadas.data.local.entity.ProyectoEntity>> = proyectoRepository
        .getAllProyectos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _proyectoSeleccionado = MutableStateFlow<com.cem.appllamadas.data.local.entity.ProyectoEntity?>(null)
    val proyectoSeleccionado: StateFlow<com.cem.appllamadas.data.local.entity.ProyectoEntity?> = _proyectoSeleccionado.asStateFlow()

    // ─── Lista filtrada por proyecto ──────────────────────────────────────────
    // Reacciona automáticamente cuando cambia el proyecto O cuando Room actualiza contactos
    val todosLosContactos: StateFlow<List<Contacto>> =
        combine(
            contactoRepository.getAllContactos(),
            _proyectoSeleccionado
        ) { lista, proyecto ->
            if (proyecto != null) {
                lista.filter { it.proyectoId == proyecto.id }
            } else {
                lista
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _contactoActual = MutableStateFlow<Contacto?>(null)
    val contactoActual: StateFlow<Contacto?> = _contactoActual.asStateFlow()

    private val _mostrarListado = MutableStateFlow(true)
    val mostrarListado: StateFlow<Boolean> = _mostrarListado.asStateFlow()

    private val _postCallState = MutableStateFlow<PostCallState?>(null)
    val postCallState: StateFlow<PostCallState?> = _postCallState.asStateFlow()

    private val _tipificaciones = MutableStateFlow<List<Tipificacion>>(emptyList())
    val tipificaciones: StateFlow<List<Tipificacion>> = _tipificaciones.asStateFlow()

    private val _historialLlamadas = MutableStateFlow<List<Llamada>>(emptyList())
    val historialLlamadas: StateFlow<List<Llamada>> = _historialLlamadas.asStateFlow()
    private var historialJob: kotlinx.coroutines.Job? = null

    private val _errorConcurrencia = MutableStateFlow<String?>(null)
    val errorConcurrencia: StateFlow<String?> = _errorConcurrencia.asStateFlow()

    init {
        observeCallState()
        syncProyectos()
        syncTipificaciones()
    }

    fun syncTipificaciones() {
        viewModelScope.launch {
            tipificacionRepository.syncTipificaciones()
            tipificacionRepository.getAll().collect { tips ->
                _tipificaciones.value = tips
            }
        }
    }

    fun syncProyectos() {
        viewModelScope.launch {
            _isLoading.value = true
            proyectoRepository.syncProyectosDesdeServidor()
            _isLoading.value = false
        }
    }

    fun seleccionarProyecto(proyecto: com.cem.appllamadas.data.local.entity.ProyectoEntity) {
        _proyectoSeleccionado.value = proyecto
        _isLoading.value = true
        viewModelScope.launch {
            // Sincronizar contactos de este proyecto desde el servidor
            contactoRepository.syncContactosDesdeServidor(proyectoId = proyecto.id)
            _isLoading.value = false
            // El Flow de observeContactos() actualizará automáticamente la lista
        }
    }

    fun deseleccionarProyecto() {
        _proyectoSeleccionado.value = null
        // todosLosContactos se actualiza automáticamente vía combine()
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
        historialJob?.cancel()
        historialJob = viewModelScope.launch {
            llamadaRepository.getHistorialByContacto(contacto.id).collect {
                _historialLlamadas.value = it
            }
        }
    }

    /** Vuelve al listado desde el detalle */
    fun volverAlListado() {
        _mostrarListado.value = true
        _contactoActual.value = null
        callStateManager.resetState()
        _postCallState.value = null
        historialJob?.cancel()
        _historialLlamadas.value = emptyList()
    }

    fun resetErrorConcurrencia() {
        _errorConcurrencia.value = null
    }

    /** Tienta bloquear el contacto en el servidor antes de llamar */
    fun intentarBloquearContacto(contactoId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = contactoRepository.lockContacto(contactoId)
            _isLoading.value = false
            
            result.onSuccess {
                onSuccess()
            }.onFailure { e ->
                if (e.message == "CONCURRENCE_ERROR") {
                    _errorConcurrencia.value = "Este contacto ya está siendo gestionado por otro agente. Por favor, selecciona otro."
                } else {
                    // Si falla por red, en este modelo de pool es crítico, no permitimos llamar
                    _errorConcurrencia.value = "Error de conexión: No se pudo verificar la exclusividad del contacto. Verifica tu internet."
                }
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            syncProyectos()
            syncTipificaciones()
            contactoRepository.syncContactosDesdeServidor()
            _isLoading.value = false
        }
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
        motivo: String?,
        observacion: String
    ) {
        val post     = _postCallState.value ?: return
        val contacto = _contactoActual.value ?: return
        val userId   = sessionManager.getUserId() ?: "desconocido"

        val hoyStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val intentoHoy = _historialLlamadas.value.any { 
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.fechaInicio)) == hoyStr 
        }

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
                motivo       = motivo,
                observacion  = observacion.ifBlank { null },
                proyectoId   = _proyectoSeleccionado.value?.id,
                listaId      = contacto.listaId,
                intentoValido = !intentoHoy
            )
            
            val tipObj = _tipificaciones.value.find { it.nombre.equals(tipificacion, ignoreCase = true) }
            val cierraCaso = tipObj?.cierraCaso ?: false

            registrarLlamadaUseCase(llamada, contacto, cierraCaso)
            _postCallState.value = null
            callStateManager.resetState()
            // Sync inmediato si hay red
            SyncWorker.dispatchImmediate(context)

            volverAlListado()
        }
    }

    /** Registra una llamada manual sin haber usado el marcador */
    fun registrarLlamadaManual(
        resultadoSeleccionado: ResultadoLlamada,
        tipificacion: String,
        motivo: String?,
        observacion: String
    ) {
        val contacto = _contactoActual.value ?: return
        val userId   = sessionManager.getUserId() ?: "desconocido"
        val ahora    = System.currentTimeMillis()

        val hoyStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ahora))
        val intentoHoy = _historialLlamadas.value.any { 
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.fechaInicio)) == hoyStr 
        }

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
                motivo       = motivo,
                observacion  = observacion.ifBlank { null },
                proyectoId   = _proyectoSeleccionado.value?.id,
                listaId      = contacto.listaId,
                intentoValido = !intentoHoy
            )
            val tipObj = _tipificaciones.value.find { it.nombre.equals(tipificacion, ignoreCase = true) }
            val cierraCaso = tipObj?.cierraCaso ?: false

            registrarLlamadaUseCase(llamada, contacto, cierraCaso)
            // Sync inmediato si hay red
            SyncWorker.dispatchImmediate(context)

            volverAlListado()
        }
    }


    fun logout() {
        sessionManager.clearSession()
    }
}
