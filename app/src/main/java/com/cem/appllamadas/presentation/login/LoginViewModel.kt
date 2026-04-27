package com.cem.appllamadas.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cem.appllamadas.data.local.AppDatabase
import com.cem.appllamadas.data.local.SessionManager
import com.cem.appllamadas.data.remote.AuthApiService
import com.cem.appllamadas.data.remote.LoginRequest
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.domain.repository.LlamadaRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Ingresa tu email y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = authApiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    sessionManager.saveSession(
                        accessToken  = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId       = body.userId,
                        nombre       = body.nombre,
                        rol          = body.rol
                    )

                    withContext(Dispatchers.IO) {
                        // PASO 1: Subir llamadas y encuestas pendientes ANTES de limpiar la BD.
                        // Esto garantiza que los datos de la sesión anterior no se pierdan
                        // incluso si la app fue cerrada antes de que el SyncWorker terminara.
                        try {
                            llamadaRepository.syncLlamadasPendientes()
                        } catch (e: Exception) {
                            e.printStackTrace() // Registro el error pero no bloqueo el login
                        }


                        // PASO 2: Solo limpiar DESPUÉS de haber subido los datos pendientes.
                        try {
                            appDatabase.clearAllTables()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // PASO 3: Descargar contactos frescos del servidor para este agente.
                        try {
                            contactoRepository.syncContactosDesdeServidor()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error("Credenciales incorrectas")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Error de conexión. Verifica tu internet.")
            }
        }
    }
}
