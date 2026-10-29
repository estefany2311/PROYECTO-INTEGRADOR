package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.ErrorNivel
import com.proyecto.movil.model.ResultadoNivel
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas del detalle (T-243) se reemplaza por una versión falsa
interface INivelRepository {
    suspend fun obtenerDetalle(nivelId: Int): ResultadoNivel
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

    private fun leerNivelAnterior(cuerpo: String?): String? = try {
        cuerpo?.let { gson.fromJson(it, ErrorNivel::class.java)?.nivelAnterior }
    } catch (e: Exception) {
        null
    }
}
