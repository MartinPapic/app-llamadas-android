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
    @Query("SELECT * FROM contacto WHERE estado != 'DESISTIDO' AND estado != 'CONTACTADO' ORDER BY intentos ASC LIMIT 1")
    suspend fun getSiguienteContacto(): ContactoEntity?

    @Query("SELECT * FROM contacto WHERE id = :id")
    suspend fun getContactoById(id: String): ContactoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacto(contacto: ContactoEntity)

    @Update
    suspend fun updateContacto(contacto: ContactoEntity)

    @Query("SELECT * FROM contacto")
    fun getAllContactos(): Flow<List<ContactoEntity>>
}
