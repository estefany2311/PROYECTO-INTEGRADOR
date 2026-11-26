package com.proyecto.movil.model.repository

import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas se reemplaza por una versión falsa
interface IMensajeRepository {
    suspend fun mensajes(): Result<List<MensajeDocente>>
}

// Los comentarios del profesor para el alumno que tiene la sesión (el backend lo sabe por el token)
class MensajeRepository : IMensajeRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    override suspend fun mensajes(): Result<List<MensajeDocente>> = try {
        val respuesta = api.mensajes()
        val lista = respuesta.body()
        // Gson puede dejar el comentario en null aunque el tipo diga String
        if (respuesta.isSuccessful && lista != null) Result.success(lista.filter { !it.comentario.isNullOrBlank() })
        else Result.failure(Exception("HTTP ${respuesta.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
