package com.proyecto.movil.model.repository

import com.proyecto.movil.model.ResultadoEstado
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas de la pantalla de espera se reemplaza por una versión falsa
interface IEvidenciaRepository {
    suspend fun estado(evidenciaId: Long): ResultadoEstado
}

// T-418: pregunta al backend estudiante (T-416) cómo va la transcripción
class EvidenciaRepository : IEvidenciaRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    override suspend fun estado(evidenciaId: Long): ResultadoEstado {
        return try {
            val respuesta = api.estadoEvidencia(evidenciaId)
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> ResultadoEstado.Exito(cuerpo)
                respuesta.code() == 401 -> ResultadoEstado.SesionVencida
                respuesta.code() == 404 -> ResultadoEstado.NoEncontrada
                else -> ResultadoEstado.SinConexion
            }
        } catch (e: Exception) {
            ResultadoEstado.SinConexion
        }
    }
}
