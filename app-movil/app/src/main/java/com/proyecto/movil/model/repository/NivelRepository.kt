package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.CompletarNivelRequest
import com.proyecto.movil.model.ErrorNivel
import com.proyecto.movil.model.ResultadoNivel
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas del detalle (T-243) se reemplaza por una versión falsa
interface INivelRepository {
    suspend fun obtenerDetalle(nivelId: Int): ResultadoNivel

    // T-216: avisa al backend que el niño terminó el reto. Con cuerpo por defecto para que
    // los repositorios falsos de las pruebas que no lo usan sigan compilando.
    suspend fun completar(nivelId: Int, puntos: Int, estrellas: Int): Result<ResultadoReto> =
        Result.failure(UnsupportedOperationException("No implementado"))
}

// T-215: el detalle lo da el backend estudiante (8082); el token lo agrega el interceptor
class NivelRepository : INivelRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)
    private val gson = Gson()

    override suspend fun obtenerDetalle(nivelId: Int): ResultadoNivel {
        return try {
            val respuesta = api.obtenerNivel(nivelId)
            val detalle = respuesta.body()
            when {
                respuesta.isSuccessful && detalle != null -> ResultadoNivel.Exito(detalle)
                respuesta.code() == 403 -> ResultadoNivel.Bloqueado(leerNivelAnterior(respuesta.errorBody()?.string()))
                respuesta.code() == 401 -> ResultadoNivel.SesionVencida
                respuesta.code() == 404 -> ResultadoNivel.NoEncontrado
                else -> ResultadoNivel.ErrorConexion
            }
        } catch (e: Exception) {
            ResultadoNivel.ErrorConexion
        }
    }

    override suspend fun completar(nivelId: Int, puntos: Int, estrellas: Int): Result<ResultadoReto> {
        return try {
            val respuesta = api.completarNivel(nivelId, CompletarNivelRequest(puntos, estrellas))
            val resultado = respuesta.body()
            if (respuesta.isSuccessful && resultado != null) Result.success(resultado)
            else Result.failure(IllegalStateException("Error ${respuesta.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun leerNivelAnterior(cuerpo: String?): String? = try {
        cuerpo?.let { gson.fromJson(it, ErrorNivel::class.java)?.nivelAnterior }
    } catch (e: Exception) {
        null
    }
}
