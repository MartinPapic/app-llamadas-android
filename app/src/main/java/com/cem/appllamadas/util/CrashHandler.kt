package com.cem.appllamadas.util

import android.content.Context
import kotlin.system.exitProcess

/**
 * Atrapa cualquier excepción no manejada que vaya a cerrar la aplicación,
 * la guarda en SharedPreferences, y luego deja que la app muera naturalmente.
 */
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val prefs = context.getSharedPreferences("app_crash_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("last_crash", exception.stackTraceToString()).commit()
        } catch (e: Exception) {
            // Ignorar si falla al guardar
        }
        
        defaultHandler?.uncaughtException(thread, exception)
        exitProcess(1)
    }
}
