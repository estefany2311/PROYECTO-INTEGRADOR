package com.proyecto.movil.model.audio

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

// T-419: grabaciones que se enviarán solas cuando haya internet; en las pruebas se usa una versión falsa
interface IColaEnvios {
    fun programar(estudianteId: Long, nivelId: Int, lecturaId: Long)
    fun cancelar(estudianteId: Long, nivelId: Int)
}

class ColaEnviosWorkManager(context: Context) : IColaEnvios {

    private val trabajos = WorkManager.getInstance(context.applicationContext)

    override fun programar(estudianteId: Long, nivelId: Int, lecturaId: Long) {
        val pedido = OneTimeWorkRequestBuilder<EnvioPendienteWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(
                EnvioPendienteWorker.ESTUDIANTE to estudianteId,
                EnvioPendienteWorker.NIVEL to nivelId,
                EnvioPendienteWorker.LECTURA to lecturaId
            ))
            .build()
        // Una sola por niño y nivel: si vuelve a grabar, la nueva reemplaza a la anterior
        trabajos.enqueueUniqueWork(nombre(estudianteId, nivelId), ExistingWorkPolicy.REPLACE, pedido)
    }

    override fun cancelar(estudianteId: Long, nivelId: Int) {
        trabajos.cancelUniqueWork(nombre(estudianteId, nivelId))
    }

    private fun nombre(estudianteId: Long, nivelId: Int) = "envio_est_${estudianteId}_nivel_$nivelId"
}
