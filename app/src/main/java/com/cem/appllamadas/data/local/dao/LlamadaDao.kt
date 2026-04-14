package com.cem.appllamadas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cem.appllamadas.data.local.entity.LlamadaEntity

@Dao
interface LlamadaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLlamada(llamada: LlamadaEntity)

    @Update
    suspend fun updateLlamada(llamada: LlamadaEntity)

    @Query("SELECT * FROM llamada WHERE pendienteSync = 1")
    suspend fun getLlamadasPendientesSync(): List<LlamadaEntity>

    @Query("UPDATE llamada SET pendienteSync = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: String)
}
