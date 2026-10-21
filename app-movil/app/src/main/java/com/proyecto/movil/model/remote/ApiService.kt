package com.proyecto.movil.model.remote

import com.proyecto.movil.model.InicioSesionRequest
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.SesionRespuesta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("api/estudiante/perfiles")
    suspend fun obtenerPerfiles(@Query("codigoAula") codigoAula: String): List<Perfil>

    // Response<> para poder leer el cuerpo de los errores 401 (PIN incorrecto) y 423 (bloqueado)
    @POST("api/estudiante/sesion")
    suspend fun iniciarSesion(@Body request: InicioSesionRequest): Response<SesionRespuesta>
}
