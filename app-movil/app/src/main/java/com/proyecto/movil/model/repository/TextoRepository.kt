package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.EnviarTextoRequest
import com.proyecto.movil.model.ResultadoEnvioTexto
import com.proyecto.movil.model.ResultadoEscritura
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.PuntajeApi
import com.proyecto.movil.model.remote.RetrofitClient

// Interfaz: en las pruebas del editor se reemplaza por una versión falsa
interface ITextoRepository {
    suspend fun enviar(nivelId: Int, texto: String): ResultadoEnvioTexto
}

// T-435 (US-21): 1) envía el texto (T-434), 2) el backend docente lo califica (US-22)
// y 3) el backend estudiante registra el intento y desbloquea el siguiente nivel
class TextoRepository : ITextoRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }
    private val docente by lazy { RetrofitClient.docente.create(PuntajeApi::class.java) }
    private val gson = Gson()

    override suspend fun enviar(nivelId: Int, texto: String): ResultadoEnvioTexto {
        val respuesta = try {
            api.enviarTexto(nivelId, EnviarTextoRequest(texto))
        } catch (e: Exception) {
            return ResultadoEnvioTexto.SinConexion
        }
        val recibido = respuesta.body()
        return when {
            respuesta.isSuccessful && recibido != null -> {
                val escritura = calificar(recibido.evidenciaId)
                val reto = if (escritura != null) completar(recibido.evidenciaId) else null
                ResultadoEnvioTexto.Exito(recibido, escritura, reto)
            }
            respuesta.code() == 401 -> ResultadoEnvioTexto.SesionVencida
            respuesta.code() in RECHAZOS -> ResultadoEnvioTexto.Rechazado(leerMensaje(respuesta.errorBody()?.string()) ?: MENSAJE_RECHAZO)
            else -> ResultadoEnvioTexto.SinConexion
        }
    }

    // Si el backend docente no responde, el texto igual quedó enviado: el puntaje se verá después
    private suspend fun calificar(evidenciaId: Long): ResultadoEscritura? = try {
        docente.evaluarEscritura(evidenciaId).body()
    } catch (e: Exception) {
        null
    }

    private suspend fun completar(evidenciaId: Long): ResultadoReto? = try {
        api.completarEscritura(evidenciaId).body()
    } catch (e: Exception) {
        null
    }

    // El backend responde los errores como {"mensaje": "..."}
    private fun leerMensaje(cuerpo: String?): String? = try {
        cuerpo?.let { gson.fromJson(it, MensajeError::class.java)?.mensaje }
    } catch (e: Exception) {
        null
    }

    private data class MensajeError(val mensaje: String?)

    companion object {
        private val RECHAZOS = setOf(400, 403, 404)
        const val MENSAJE_RECHAZO = "No pudimos enviar tu texto. Revísalo e inténtalo otra vez."
    }
}
