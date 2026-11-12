package com.proyecto.movil.model.remote

import com.proyecto.movil.model.CompletarNivelRequest
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.InicioSesionRequest
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.model.NivelDetalle
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.RespuestaOrtografia
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.model.VerificarTextoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/estudiante/perfiles")
    suspend fun obtenerPerfiles(@Query("codigoAula") codigoAula: String): List<Perfil>

    // Response<> para poder leer el cuerpo de los errores 401 (PIN incorrecto) y 423 (bloqueado)
    @POST("api/estudiante/sesion")
    suspend fun iniciarSesion(@Body request: InicioSesionRequest): Response<SesionRespuesta>

    // T-236: invalida el token en el backend. El token lo pone solo el interceptor de RetrofitClient.
    @DELETE("api/estudiante/sesion")
    suspend fun cerrarSesion(): Response<Unit>

    // T-215: detalle de un nivel (T-214). Response<> para leer el 403 de un nivel bloqueado
    @GET("api/estudiante/niveles/{nivelId}")
    suspend fun obtenerNivel(@Path("nivelId") nivelId: Int): Response<NivelDetalle>

    // T-219: el texto que el niño lee en el nivel (T-218). Response<> para leer el 403 y el 404
    @GET("api/estudiante/niveles/{nivelId}/lectura")
    suspend fun obtenerLectura(@Path("nivelId") nivelId: Int): Response<LecturaNivel>

    // T-321: la consigna de un nivel de escritura (T-320). Response<> para leer el 403 y el 404
    @GET("api/estudiante/niveles/{nivelId}/consigna")
    suspend fun obtenerConsigna(@Path("nivelId") nivelId: Int): Response<ConsignaNivel>

    // T-330: errores de ortografía del texto del editor (T-328)
    @POST("api/estudiante/ortografia/verificar")
    suspend fun verificarOrtografia(@Body request: VerificarTextoRequest): Response<RespuestaOrtografia>

    // T-216: terminar un reto (T-198/T-309). Al volver, el mapa se refresca y anima lo desbloqueado
    @POST("api/estudiante/niveles/{nivelId}/completar")
    suspend fun completarNivel(
        @Path("nivelId") nivelId: Int,
        @Body request: CompletarNivelRequest
    ): Response<ResultadoReto>
}
