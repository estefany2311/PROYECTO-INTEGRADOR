package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.ErrorNivel
import com.proyecto.movil.model.ResultadoLectura
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas de la pantalla de lectura (T-312) se reemplaza por una versión falsa
interface ILecturaRepository {
    suspend fun obtenerLectura(nivelId: Int): ResultadoLectura
}

// T-219: la lectura la da el backend estudiante (T-218); el token lo agrega el interceptor
class LecturaRepository : ILecturaRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)
    private val gson = Gson()

    override suspend fun obtenerLectura(nivelId: Int): ResultadoLectura {
        return try {
            val respuesta = api.obtenerLectura(nivelId)
            val lectura = respuesta.body()
            when {
                respuesta.isSuccessful && lectura != null -> ResultadoLectura.Exito(lectura)
                respuesta.code() == 403 -> ResultadoLectura.Bloqueado(leerNivelAnterior(respuesta.errorBody()?.string()))
                respuesta.code() == 404 -> ResultadoLectura.SinLectura
                respuesta.code() == 401 -> ResultadoLectura.SesionVencida
                else -> ResultadoLectura.ErrorConexion
            }
        } catch (e: Exception) {
            ResultadoLectura.ErrorConexion
        }
    }

    private fun leerNivelAnterior(cuerpo: String?): String? = try {
        cuerpo?.let { gson.fromJson(it, ErrorNivel::class.java)?.nivelAnterior }
    } catch (e: Exception) {
        null
    }
}
