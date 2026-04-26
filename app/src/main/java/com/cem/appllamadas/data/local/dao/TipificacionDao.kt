package com.cem.appllamadas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cem.appllamadas.data.local.entity.TipificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TipificacionDao {
    @Query("SELECT * FROM tipificacion")
    fun getAll(): Flow<List<TipificacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tipificaciones: List<TipificacionEntity>)
}
