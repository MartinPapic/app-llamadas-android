package com.cem.appllamadas.data.local.dao

import androidx.room.*
import com.cem.appllamadas.data.local.entity.ProyectoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProyectoDao {
    @Query("SELECT * FROM proyecto ORDER BY nombre ASC")
    fun getAllProyectos(): Flow<List<ProyectoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProyectos(proyectos: List<ProyectoEntity>)

    @Query("DELETE FROM proyecto")
    suspend fun deleteAllProyectos()
}
