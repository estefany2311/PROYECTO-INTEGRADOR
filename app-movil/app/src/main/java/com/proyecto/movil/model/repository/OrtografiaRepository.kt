package com.proyecto.movil.model.repository

import com.proyecto.movil.model.ResultadoOrtografia
import com.proyecto.movil.model.VerificarTextoRequest
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas del editor se reemplaza por una versión falsa
interface IOrtografiaRepository {
    suspend fun verificar(texto: String): ResultadoOrtografia
}

// T-330: el corrector corre en el backend estudiante (T-328); el token lo agrega el interceptor
class OrtografiaRepository : IOrtografiaRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    override suspend fun verificar(texto: String): ResultadoOrtografia {
        return try {
            val respuesta = api.verificarOrtografia(VerificarTextoRequest(texto))
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoOrtografia.Exito(cuerpo.errores)
                respuesta.code() == 401 -> ResultadoOrtografia.SesionVencida
                else -> ResultadoOrtografia.SinConexion
            }
        } catch (e: Exception) {
            ResultadoOrtografia.SinConexion
        }
    }
}
