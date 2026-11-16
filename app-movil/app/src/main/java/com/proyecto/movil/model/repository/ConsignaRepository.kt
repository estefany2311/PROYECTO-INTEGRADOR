package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.ErrorNivel
import com.proyecto.movil.model.ResultadoConsigna
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas de la pantalla de consigna (T-322) se reemplaza por una versión falsa
interface IConsignaRepository {
    suspend fun obtenerConsigna(nivelId: Int): ResultadoConsigna
}

// T-321: la consigna la da el backend estudiante (T-320); el token lo agrega el interceptor
class ConsignaRepository : IConsignaRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)
    private val gson = Gson()

    override suspend fun obtenerConsigna(nivelId: Int): ResultadoConsigna {
        return try {
            val respuesta = api.obtenerConsigna(nivelId)
            val consigna = respuesta.body()
            when {
                respuesta.isSuccessful && consigna != null -> ResultadoConsigna.Exito(consigna)
                respuesta.code() == 403 -> ResultadoConsigna.Bloqueado(leerNivelAnterior(respuesta.errorBody()?.string()))
                respuesta.code() == 404 -> ResultadoConsigna.SinConsigna
                respuesta.code() == 401 -> ResultadoConsigna.SesionVencida
                else -> ResultadoConsigna.ErrorConexion
            }
        } catch (e: Exception) {
            ResultadoConsigna.ErrorConexion
        }
    }

    private fun leerNivelAnterior(cuerpo: String?): String? = try {
        cuerpo?.let { gson.fromJson(it, ErrorNivel::class.java)?.nivelAnterior }
    } catch (e: Exception) {
        null
    }
}
