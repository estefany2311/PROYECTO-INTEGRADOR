package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.ResultadoEstado
import com.proyecto.movil.model.ResultadoEvaluacion
import com.proyecto.movil.model.repository.EvidenciaRepository
import com.proyecto.movil.model.repository.IEvidenciaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// T-418: lo que muestra la pantalla mientras la IA escucha la lectura
sealed class EsperaUiState {
    object Escuchando : EsperaUiState()
    data class Lista(val transcripcion: String) : EsperaUiState()      // listo, pero sin poder leer el resultado
    data class Resultado(val resultado: ResultadoEvaluacion) : EsperaUiState()   // T-426
    data class Error(val mensaje: String) : EsperaUiState()
    object TardaMucho : EsperaUiState()   // pasó el tiempo máximo; el profesor igual la recibe
}

// T-418 (US-13): pregunta cada 2 segundos si la IA ya terminó (T-416)
class EsperaViewModel(
    private val evidencias: IEvidenciaRepository = EvidenciaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<EsperaUiState>(EsperaUiState.Escuchando)
    val uiState: StateFlow<EsperaUiState> = _uiState

    private var consulta: Job? = null

    fun seguir(evidenciaId: Long) {
        if (consulta?.isActive == true || _uiState.value !is EsperaUiState.Escuchando) return
        consulta = viewModelScope.launch {
            var esperado = 0L
            while (true) {
                val resultado = evidencias.estado(evidenciaId)
                val siguiente = when (resultado) {
                    is ResultadoEstado.Exito -> when (resultado.estado.estado) {
                        LISTO -> conResultado(evidenciaId, resultado.estado.transcripcion.orEmpty())
                        ERROR -> EsperaUiState.Error(MENSAJE_ERROR_IA)
                        else -> null   // sigue procesando
                    }
                    ResultadoEstado.SesionVencida -> EsperaUiState.Error(MENSAJE_SESION_VENCIDA)
                    ResultadoEstado.NoEncontrada -> EsperaUiState.Error(MENSAJE_NO_ENCONTRADA)
                    ResultadoEstado.SinConexion -> null   // se vuelve a intentar en la siguiente vuelta
                }
                if (siguiente != null) {
                    _uiState.value = siguiente
                    return@launch
                }
                if (esperado >= ESPERA_MAXIMA_MS) {
                    _uiState.value = EsperaUiState.TardaMucho
                    return@launch
                }
                delay(INTERVALO_MS)
                esperado += INTERVALO_MS
            }
        }
    }

    // T-426: pide las estrellas y avisa al backend docente; si no se puede, muestra solo lo que escuchó
    private suspend fun conResultado(evidenciaId: Long, transcripcion: String): EsperaUiState {
        val resultado = evidencias.resultado(evidenciaId)
        if (resultado == null || resultado.estado != LISTO) return EsperaUiState.Lista(transcripcion)
        viewModelScope.launch { evidencias.avisarDocente(evidenciaId, resultado.asignacionId) }
        return EsperaUiState.Resultado(resultado)
    }

    companion object {
        const val LISTO = "listo"
        const val ERROR = "error"
        const val INTERVALO_MS = 2_000L
        const val ESPERA_MAXIMA_MS = 180_000L   // 3 minutos: Whisper en CPU tarda con lecturas largas
        const val MENSAJE_ERROR_IA = "No pudimos escuchar bien tu lectura. Tu profesor igual la va a escuchar."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Tu lectura sí se envió."
        const val MENSAJE_NO_ENCONTRADA = "No encontramos tu grabación. Intenta grabar otra vez."
    }
}
