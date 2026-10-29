package com.proyecto.movil.model.remote

import com.proyecto.movil.model.MapaRespuesta
import retrofit2.http.GET

/**
 * T-210: el mapa lo da el backend DOCENTE (Noemí, puerto 8081), no el de estudiante.
 * Por eso va en su propia interfaz, con otra dirección base (RetrofitClient.docente).
 *
 * No lleva el id del alumno: el backend lo saca del token (T-197, commit fddf419).
 * El token "Authorization: Bearer ..." lo agrega solo el interceptor de RetrofitClient.
 */
interface MapaApi {
    @GET("api/estudiante/mapa")
    suspend fun obtenerMapa(): MapaRespuesta
}
