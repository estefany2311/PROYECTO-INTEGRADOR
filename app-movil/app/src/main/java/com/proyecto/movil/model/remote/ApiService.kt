package com.proyecto.movil.model.remote

import com.proyecto.movil.model.Perfil
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/estudiante/perfiles")
    suspend fun obtenerPerfiles(@Query("codigoAula") codigoAula: String): List<Perfil>
}