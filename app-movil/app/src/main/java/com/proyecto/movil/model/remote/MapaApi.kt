package com.proyecto.movil.model.remote

import com.proyecto.movil.model.MapaRespuesta
import retrofit2.http.GET

interface MapaApi {
    @GET("api/estudiante/mapa")
    suspend fun obtenerMapa(): MapaRespuesta
}
