@file:Suppress("DEPRECATION")
package com.cem.appllamadas.call

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.cem.appllamadas.domain.model.ResultadoLlamada
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CallResult(
    val resultado: ResultadoLlamada,
    val duracion: Int,       // seconds
    val fechaInicio: Long,
    val fechaFin: Long
)

sealed class CallState {
    object Idle : CallState()
    object Calling : CallState()           // call initiated, waiting
    object Answered : CallState()          // OFFHOOK detected
    data class Ended(val result: CallResult) : CallState()
}

@Singleton
class CallStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var callStartTime: Long = 0L
    private var wasAnswered: Boolean = false
    private var isTracking: Boolean = false

    private val listener = object : PhoneStateListener() {
        @Suppress("DEPRECATION")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (!isTracking) return

            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call connected / answered
                    wasAnswered = true
                    callStartTime = System.currentTimeMillis()
                    _callState.value = CallState.Answered
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended
                    val endTime = System.currentTimeMillis()
                    val duracion = if (wasAnswered)
                        ((endTime - callStartTime) / 1000).toInt()
                    else 0

                    val resultado = if (wasAnswered)
                        ResultadoLlamada.CONTESTA
                    else
                        ResultadoLlamada.NO_CONTESTA

                    _callState.value = CallState.Ended(
                        CallResult(
                            resultado = resultado,
                            duracion = duracion,
                            fechaInicio = if (wasAnswered) callStartTime else endTime,
                            fechaFin = endTime
                        )
                    )
                    stopTracking()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun startTracking() {
        if (isTracking) return
        wasAnswered = false
        callStartTime = System.currentTimeMillis()
        isTracking = true
        _callState.value = CallState.Calling
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private fun stopTracking() {
        isTracking = false
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
    }

    fun resetState() {
        _callState.value = CallState.Idle
    }
}
