package com.proyecto.movil.model.remote

import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

// T-426: endpoints del backend docente (Noemí) que la app llama al terminar una lectura
interface PuntajeApi {
    // Suma la lectura al total de puntos y estrellas del mapa (idempotente)
    @POST("api/estudiante/evidencias/{evidenciaId}/lectura")
    suspend fun contarLectura(@Path("evidenciaId") evidenciaId: Long): Response<Unit>

    // US-38: marca la tarea del docente como entregada (idempotente)
    @POST("api/estudiante/asignaciones/{asignacionId}/entregar")
    suspend fun entregarTarea(@Path("asignacionId") asignacionId: Long): Response<Unit>
}
