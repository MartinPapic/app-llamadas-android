package com.cem.appllamadas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cem.appllamadas.data.local.dao.ContactoDao
import com.cem.appllamadas.data.local.dao.LlamadaDao
import com.cem.appllamadas.data.local.entity.ContactoEntity
import com.cem.appllamadas.data.local.entity.LlamadaEntity

@Database(
    entities = [ContactoEntity::class, LlamadaEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val contactoDao: ContactoDao
    abstract val llamadaDao: LlamadaDao
}
