package com.proyecto.movil.model.audio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.ResultadoEnvio
import com.proyecto.movil.model.repository.AudioRepository
import com.proyecto.movil.model.repository.IAudioRepository

// T-419 (US-13): sube en segundo plano una grabación que quedó pendiente por falta de internet
class EnvioPendienteWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result = enviarPendiente(
        estudianteId = inputData.getLong(ESTUDIANTE, 0L),
        nivelId = inputData.getInt(NIVEL, 0),
        lecturaId = inputData.getLong(LECTURA, 0L),
        intento = runAttemptCount,
        almacen = Dependencias.almacenGrabaciones,
        sesionActual = { Dependencias.almacenSesion.leer()?.estudianteId },
        audios = AudioRepository()
    )

    companion object {
        const val ESTUDIANTE = "estudianteId"
        const val NIVEL = "nivelId"
        const val LECTURA = "lecturaId"
        const val INTENTOS_MAXIMOS = 10

        // Separado del Worker para poder probarlo sin WorkManager
        suspend fun enviarPendiente(
            estudianteId: Long,
            nivelId: Int,
            lecturaId: Long,
            intento: Int,
            almacen: AlmacenGrabaciones,
            sesionActual: () -> Long?,
            audios: IAudioRepository
        ): Result {
            val grabacion = almacen.guardada(estudianteId, nivelId) ?: return Result.success()   // ya no hay nada que enviar
            // El token es del niño que tiene la sesión abierta: si es otro, se espera a que vuelva a entrar
            if (sesionActual() != estudianteId) return reintentar(intento)
            return when (audios.enviar(nivelId, lecturaId, grabacion.archivo) {}) {
                is ResultadoEnvio.Exito -> {
                    almacen.borrar(estudianteId, nivelId)
                    Result.success()
                }
                ResultadoEnvio.SinConexion -> reintentar(intento)
                // Rechazada o sesión vencida: la grabación sigue en la tablet y el niño la puede enviar desde el nivel
                is ResultadoEnvio.Rechazado, ResultadoEnvio.SesionVencida -> Result.failure()
            }
        }

        private fun reintentar(intento: Int): Result =
            if (intento + 1 >= INTENTOS_MAXIMOS) Result.failure() else Result.retry()
    }
}
