package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.ErrorApi
import com.proyecto.movil.model.InicioSesionRequest
import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient
import retrofit2.Response

interface ISesionRepository {
    suspend fun iniciarSesion(estudianteId: Long, pin: String): ResultadoLogin
}

class SesionRepository : ISesionRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)
    private val gson = Gson()

    override suspend fun iniciarSesion(estudianteId: Long, pin: String): ResultadoLogin {
        return try {
            val respuesta = api.iniciarSesion(InicioSesionRequest(estudianteId, pin))
            val sesion = respuesta.body()
            when {
                respuesta.isSuccessful && sesion != null -> ResultadoLogin.Exito(sesion)
                respuesta.code() == 401 -> ResultadoLogin.PinIncorrecto(leerError(respuesta)?.intentosRestantes)
                respuesta.code() == 423 -> ResultadoLogin.Bloqueado(leerError(respuesta)?.segundosRestantes)
                else -> ResultadoLogin.ErrorConexion(leerError(respuesta)?.mensaje ?: "Error ${respuesta.code()}")
            }
        } catch (e: Exception) {
            ResultadoLogin.ErrorConexion(e.message ?: "Error de conexión")
        }
    }

    // El cuerpo de error viene como texto JSON: {"mensaje": "...", "intentosRestantes": 3}
    private fun leerError(respuesta: Response<*>): ErrorApi? {
        return try {
            respuesta.errorBody()?.string()?.let { gson.fromJson(it, ErrorApi::class.java) }
        } catch (e: Exception) {
            null
        }
    }
}
