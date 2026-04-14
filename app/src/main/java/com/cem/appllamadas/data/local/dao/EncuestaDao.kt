package com.cem.appllamadas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cem.appllamadas.data.local.entity.EncuestaEntity

@Dao
interface EncuestaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncuesta(encuesta: EncuestaEntity)

    @Update
    suspend fun updateEncuesta(encuesta: EncuestaEntity)

    @Query("SELECT * FROM encuesta WHERE pendienteSync = 1")
    suspend fun getEncuestasNoSincronizadas(): List<EncuestaEntity>

    @Query("UPDATE encuesta SET pendienteSync = 0 WHERE id IN (:ids)")
    suspend fun marcarSincronizadas(ids: List<String>)
}
