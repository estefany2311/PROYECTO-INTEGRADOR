package com.proyecto.movil.model.repository

import com.google.gson.Gson
import com.proyecto.movil.model.AudioRecibido
import com.proyecto.movil.model.ResultadoEnvio
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.CuerpoConProgreso
import com.proyecto.movil.model.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

// Interfaz: en las pruebas del ViewModel se reemplaza por una versión falsa
interface IAudioRepository {
    suspend fun enviar(nivelId: Int, lecturaId: Long, archivo: File, alAvanzar: (Float) -> Unit): ResultadoEnvio
}

// T-417 (US-13): sube la grabación al backend estudiante (T-413/T-414); el token lo pone el interceptor
class AudioRepository : IAudioRepository {
    private val api by lazy { RetrofitClient.instance.create(ApiService::class.java) }
    private val gson = Gson()

    override suspend fun enviar(nivelId: Int, lecturaId: Long, archivo: File, alAvanzar: (Float) -> Unit): ResultadoEnvio {
        return try {
            val cuerpo = CuerpoConProgreso(archivo, "audio/wav".toMediaType(), alAvanzar)
            val audio = MultipartBody.Part.createFormData("audio", archivo.name, cuerpo)
            val lectura = lecturaId.toString().toRequestBody("text/plain".toMediaType())
            val respuesta = api.subirAudio(nivelId, lectura, audio)
            val recibido = respuesta.body()
            when {
                respuesta.isSuccessful && recibido != null -> ResultadoEnvio.Exito(recibido)
                respuesta.code() == 401 -> ResultadoEnvio.SesionVencida
                respuesta.code() in RECHAZOS -> ResultadoEnvio.Rechazado(leerMensaje(respuesta) ?: MENSAJE_RECHAZO)
                else -> ResultadoEnvio.SinConexion
            }
        } catch (e: Exception) {
            ResultadoEnvio.SinConexion
        }
    }

    // El backend responde los errores como {"mensaje": "..."}
    private fun leerMensaje(respuesta: Response<AudioRecibido>): String? = try {
        respuesta.errorBody()?.string()?.let { gson.fromJson(it, MensajeError::class.java)?.mensaje }
    } catch (e: Exception) {
        null
    }

    private data class MensajeError(val mensaje: String?)

    companion object {
        private val RECHAZOS = setOf(400, 403, 404, 413, 415)
        const val MENSAJE_RECHAZO = "No pudimos usar esta grabación. Vuelve a grabar."
    }
}
