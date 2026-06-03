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
            CASE 
                WHEN ultimaTipificacion LIKE '%Llamar m_s tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%llamar m_s tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%Llamar mas tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%llamar mas tarde%' THEN 0
                WHEN estado = 'PENDIENTE' THEN 1 
                ELSE 2 
            END ASC,
            ordenAleatorio ASC,
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

    @Query("DELETE FROM contacto WHERE proyectoId = :proyectoId")
    suspend fun deleteContactosByProyecto(proyectoId: String)

    @Query("""
        SELECT * FROM contacto 
        ORDER BY 
            CASE 
                WHEN ultimaTipificacion LIKE '%Llamar m_s tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%llamar m_s tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%Llamar mas tarde%' THEN 0
                WHEN ultimaTipificacion LIKE '%llamar mas tarde%' THEN 0
                WHEN estado = 'PENDIENTE' THEN 1 
                WHEN estado = 'EN_GESTION' THEN 2 
                WHEN estado = 'CONTACTADO' THEN 3 
                ELSE 4 
            END ASC,
            ordenAleatorio ASC,
            intentos ASC,
            IFNULL(fechaUltimaGestion, 0) ASC
    """)
    fun getAllContactos(): Flow<List<ContactoEntity>>

    @Query("SELECT * FROM contacto")
    suspend fun getAllContactosList(): List<ContactoEntity>
}
