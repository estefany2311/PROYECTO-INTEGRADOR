package com.proyecto.movil.model.repository

import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.remote.MapaApi
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas del mapa (T-212) se reemplaza por una versión falsa
interface IMapaRepository {
    // El mapa del alumno que tiene la sesión abierta (el backend lo identifica por el token)
    suspend fun obtenerMapa(): Result<MapaRespuesta>
}

class MapaRepository : IMapaRepository {
    private val api = RetrofitClient.docente.create(MapaApi::class.java)

    override suspend fun obtenerMapa(): Result<MapaRespuesta> {
        return try {
            Result.success(api.obtenerMapa())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
