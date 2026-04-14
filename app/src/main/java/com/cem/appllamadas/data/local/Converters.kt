package com.cem.appllamadas.data.local

import androidx.room.TypeConverter
import com.cem.appllamadas.domain.model.EstadoContacto
import com.cem.appllamadas.domain.model.ResultadoLlamada

class Converters {
    @TypeConverter
    fun fromEstadoContacto(value: EstadoContacto?): String? = value?.name

    @TypeConverter
    fun toEstadoContacto(value: String?): EstadoContacto? = value?.let { EstadoContacto.valueOf(it) }

    @TypeConverter
    fun fromResultadoLlamada(value: ResultadoLlamada?): String? = value?.name

    @TypeConverter
    fun toResultadoLlamada(value: String?): ResultadoLlamada? = value?.let { ResultadoLlamada.valueOf(it) }
}
