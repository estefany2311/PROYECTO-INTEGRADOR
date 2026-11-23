package com.proyecto.movil.model.repository

import com.proyecto.movil.model.ResultadoEstado
import com.proyecto.movil.model.ResultadoEvaluacion
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.PuntajeApi
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas de la pantalla de espera se reemplaza por una versión falsa
interface IEvidenciaRepository {
    suspend fun estado(evidenciaId: Long): ResultadoEstado

    // T-426: null si no se pudo leer (sin internet o error)
    suspend fun resultado(evidenciaId: Long): ResultadoEvaluacion? = null

    // T-426: avisa al backend docente que cuente la lectura y, si era una tarea, que quedó entregada
    suspend fun avisarDocente(evidenciaId: Long, asignacionId: Long?) {}
}

// T-418: pregunta al backend estudiante (T-416) cómo va la transcripción
class EvidenciaRepository : IEvidenciaRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }
    private val docente by lazy { RetrofitClient.docente.create(PuntajeApi::class.java) }

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

    override suspend fun resultado(evidenciaId: Long): ResultadoEvaluacion? = try {
        api.resultadoLectura(evidenciaId).body()
    } catch (e: Exception) {
        null
    }

    // Los dos endpoints son idempotentes: si fallan, el backend docente igual cuenta las lecturas pendientes
    override suspend fun avisarDocente(evidenciaId: Long, asignacionId: Long?) {
        try {
            docente.contarLectura(evidenciaId)
            asignacionId?.let { docente.entregarTarea(it) }
        } catch (e: Exception) {
            // sin conexión con el backend docente: no se le muestra nada al niño
        }
    }
}
