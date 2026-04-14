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

        // Seed test contacts on first launch
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("db_seeded", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                val testContacts = listOf(
                    ContactoEntity("c001", "Ana Garcia",       "3001234567", EstadoContacto.PENDIENTE, 0, System.currentTimeMillis()),
                    ContactoEntity("c002", "Carlos Perez",     "3109876543", EstadoContacto.PENDIENTE, 0, System.currentTimeMillis()),
                    ContactoEntity("c003", "Maria Lopez",      "3154567890", EstadoContacto.PENDIENTE, 0, System.currentTimeMillis()),
                    ContactoEntity("c004", "Luis Martinez",    "3207654321", EstadoContacto.PENDIENTE, 0, System.currentTimeMillis()),
                    ContactoEntity("c005", "Sandra Rodriguez", "3001112233", EstadoContacto.PENDIENTE, 0, System.currentTimeMillis())
                )
                testContacts.forEach { database.contactoDao.insertContacto(it) }
                prefs.edit().putBoolean("db_seeded", true).apply()
            }
        }
    }
}
