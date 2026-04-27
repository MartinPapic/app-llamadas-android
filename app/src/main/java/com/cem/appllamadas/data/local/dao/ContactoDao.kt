package com.cem.appllamadas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cem.appllamadas.data.local.entity.ContactoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {
    @Query("""
        SELECT * FROM contacto 
        WHERE estado NOT IN ('DESISTIDO', 'CONTACTADO') 
        ORDER BY 
            CASE WHEN estado = 'PENDIENTE' THEN 0 ELSE 1 END ASC,
            intentos ASC,
            IFNULL(fechaUltimaGestion, 0) ASC
        LIMIT 1
    """)
    suspend fun getSiguienteContacto(): ContactoEntity?

    @Query("SELECT * FROM contacto WHERE id = :id")
    suspend fun getContactoById(id: String): ContactoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacto(contacto: ContactoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactos(contactos: List<ContactoEntity>)

    @Update
    suspend fun updateContacto(contacto: ContactoEntity)

    @Query("""
        SELECT * FROM contacto 
        ORDER BY 
            CASE 
                WHEN estado = 'PENDIENTE' THEN 0 
                WHEN estado = 'EN_GESTION' THEN 1 
                WHEN estado = 'CONTACTADO' THEN 2 
                ELSE 3 
            END ASC,
            intentos ASC,
            IFNULL(fechaUltimaGestion, 0) ASC
    """)
    fun getAllContactos(): Flow<List<ContactoEntity>>
}
