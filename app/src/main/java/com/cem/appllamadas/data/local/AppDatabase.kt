package com.cem.appllamadas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cem.appllamadas.data.local.dao.ContactoDao
import com.cem.appllamadas.data.local.dao.LlamadaDao
import com.cem.appllamadas.data.local.entity.ContactoEntity
import com.cem.appllamadas.data.local.entity.LlamadaEntity
import com.cem.appllamadas.data.local.dao.EncuestaDao
import com.cem.appllamadas.data.local.entity.EncuestaEntity

@Database(
    entities = [ContactoEntity::class, LlamadaEntity::class, EncuestaEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val contactoDao: ContactoDao
    abstract val llamadaDao: LlamadaDao
    abstract val encuestaDao: EncuestaDao
}
