package com.proyecto.movil.model.repository

import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

class PerfilRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)

    suspend fun obtenerPerfiles(codigoAula: String): Result<List<Perfil>> {
        return try {
            Result.success(api.obtenerPerfiles(codigoAula))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}