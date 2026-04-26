package com.cem.appllamadas.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cem.appllamadas.domain.repository.LlamadaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker de sincronización offline-first.
 *
 * Estrategia:
 * 1. Lee llamadas con pendienteSync = true en Room
 * 2. Las envía al backend en batch via POST /api/sync
 * 3. Si el backend confirma → marca como pendienteSync = false
 * 4. Si falla → WorkManager hace retry con backoff exponencial (30s, 1m, 2m…)
 *
 * Se ejecuta cada 15 min cuando hay red disponible (WorkManager + Constraints)
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val llamadaRepository: LlamadaRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID   = "sync_channel"
        private const val NOTIF_ID     = 1001
        private const val WORK_NAME    = "sync_llamadas"

        /** Programa el worker periódico con constraints de red */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Dispara sync inmediato (por ej. al guardar una llamada con red disponible) */
        fun dispatchImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            llamadaRepository.syncLlamadasPendientes()
            // Notificar éxito solo si hay datos sincronizados
            // (el repo no lanza excepción si no hay pendientes)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Si se alcanzó el límite de reintentos, fallo definitivo
            if (runAttemptCount >= 3) {
                showSyncFailedNotification()
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun showSyncFailedNotification() {
        val notifManager = appContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notifManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Error de sincronización")
            .setContentText("No se pudieron enviar algunas llamadas. Se reintentará cuando haya conexión.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notifManager.notify(NOTIF_ID, notification)
    }
}
