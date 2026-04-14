package com.cem.appllamadas

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cem.appllamadas.data.local.AppDatabase
import com.cem.appllamadas.data.local.entity.ContactoEntity
import com.cem.appllamadas.domain.model.EstadoContacto

@HiltAndroidApp
class AppLlamadasApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var database: AppDatabase

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
            
    override fun onCreate() {
        super.onCreate()
        
        // Programar sync periódico con red (evita duplicados con UniquePeriodicWork)
        com.cem.appllamadas.worker.SyncWorker.schedule(this)

    }
}
