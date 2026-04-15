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
    object Idle     : CallState()
    object Calling  : CallState()    // call initiated (dialing)
    object Answered : CallState()    // OFFHOOK — call connected
    data class Ended(val result: CallResult) : CallState()
}

/**
 * Tracks the lifecycle of a phone call using PhoneStateListener.
 *
 * KEY FIX: when PhoneStateListener is registered, Android immediately broadcasts
 * the current phone state (usually IDLE). We MUST ignore this initial IDLE event
 * or the post-call form will appear instantly with duration = 0.
 *
 * We only process IDLE after having seen at least one OFFHOOK event, which
 * signals that the call actually connected (or at least the dialer went active).
 */
@Singleton
class CallStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Time when the user pressed "Llamar"
    private var trackingStartTime: Long = 0L
    // Time when OFFHOOK was detected (call answered)
    private var offhookTime: Long = 0L

    private var wasAnswered: Boolean = false
    private var isTracking: Boolean = false

    /**
     * Guard against the initial IDLE broadcast that PhoneStateListener fires
     * on registration. We only process IDLE after we've seen an active state.
     */
    private var hasSeenActiveState: Boolean = false

    private val listener = object : PhoneStateListener() {
        @Suppress("DEPRECATION")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (!isTracking) return

            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call is active (connected). Record exact answer time.
                    hasSeenActiveState = true
                    wasAnswered = true
                    offhookTime = System.currentTimeMillis()
                    _callState.value = CallState.Answered
                }

                TelephonyManager.CALL_STATE_RINGING -> {
                    // Outgoing call is ringing on the other side.
                    // Mark as active so IDLE after this is not ignored.
                    hasSeenActiveState = true
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    // IMPORTANT: ignore the very first IDLE that fires on listener
                    // registration. Only process IDLE after we've seen an active state.
                    if (!hasSeenActiveState) return

                    val endTime = System.currentTimeMillis()

                    // Calculate duration from the moment the call was answered (OFFHOOK),
                    // falling back to tracking start if OFFHOOK wasn't captured.
                    val (duracion, fechaInicio) = if (wasAnswered) {
                        val secs = ((endTime - offhookTime) / 1000).toInt()
                        Pair(secs, offhookTime)
                    } else {
                        // OFFHOOK was never captured (can happen on some Android 12+ devices).
                        // Use trackingStartTime as a proxy — agent pressed Call, then the
                        // call went active and ended, so elapsed time ≈ call duration.
                        val elapsedSecs = ((endTime - trackingStartTime) / 1000).toInt()
                        Pair(elapsedSecs, trackingStartTime)
                    }

                    // If the call ended without OFFHOOK and very quickly,
                    // it was likely not answered (busy, rejected, etc.)
                    val resultado = if (wasAnswered || duracion >= 5)
                        ResultadoLlamada.CONTACTADO_EFECTIVO
                    else
                        ResultadoLlamada.NO_CONTACTADO

                    _callState.value = CallState.Ended(
                        CallResult(
                            resultado   = resultado,
                            duracion    = duracion,
                            fechaInicio = fechaInicio,
                            fechaFin    = endTime
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
        // Reset all state before starting
        wasAnswered       = false
        hasSeenActiveState = false
        offhookTime       = 0L
        trackingStartTime = System.currentTimeMillis()
        isTracking        = true
        _callState.value  = CallState.Calling
        // Register listener — Android will immediately fire the current IDLE state.
        // The hasSeenActiveState guard above will properly ignore it.
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
