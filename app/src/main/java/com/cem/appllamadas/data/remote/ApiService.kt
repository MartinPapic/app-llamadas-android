package com.cem.appllamadas.data.remote

import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.model.Llamada
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ─── DTOs de sincronización ───────────────────────────────────────────────────

data class SyncPayload(
    val llamadas: List<LlamadaDto>,
    val contactosActualizados: List<ContactoDto>
)

data class SyncResponse(
    val success: Boolean,
    val message: String,
    val synchronizedIds: List<String>
)

data class LlamadaDto(
    val id: String,
    val contactoId: String,
    val usuarioId: String,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val duracion: Int?,
    val resultado: String?,      // "CONTACTADO_EFECTIVO", "CONTACTADO_NO_EFECTIVO", "NO_CONTACTADO"
    val tipificacion: String?,
    val motivo: String?,
    val observacion: String?,
    val proyectoId: String?
)

data class ProyectoDto(
    val id: String,
    val nombre: String,
    val estado: String,
    val fechaCreacion: Long
)

data class ContactoDto(
    val id: String,
    val nombre: String,
    val telefono: String,
    val estado: String,          // "pendiente", "en_gestion", "contactado", "desistido"
    val intentos: Int,
    val fechaCreacion: Long
)

data class TipificacionDto(
    val id: String,
    val nombre: String,
    val resultado: String,
    val cierraCaso: Boolean
)


// ─── API ──────────────────────────────────────────────────────────────────────

interface ApiService {

    /** Sincronización batch: envía N llamadas y contactos actualizados de una vez */
    @POST("/sync")
    suspend fun syncData(@Body payload: SyncPayload): Response<SyncResponse>

    /** Registrar una sola llamada (para sync inmediato si hay conexión) */
    @POST("/calls")
    suspend fun registrarLlamada(@Body llamada: LlamadaDto): Response<Map<String, String>>

    /** Obtener contactos del servidor (opcional: sincronizar hacia abajo) */
    @GET("/contacts")
    suspend fun getContactos(
        @Query("estado") estado: String? = null,
        @Query("proyectoId") proyectoId: String? = null
    ): Response<List<ContactoDto>>

    /** Bloqueo preventivo de contacto antes de llamar ( Pool Model ) */
    @POST("/contacts/{id}/lock")
    suspend fun lockContacto(@retrofit2.http.Path("id") id: String): Response<Map<String, Any>>

    /** Obtener proyectos asignados al agente */
    @GET("/projects/agente")
    suspend fun getProyectosAgente(): Response<List<ProyectoDto>>

    @GET("/tipifications")
    suspend fun getTipificaciones(): Response<List<TipificacionDto>>
}
