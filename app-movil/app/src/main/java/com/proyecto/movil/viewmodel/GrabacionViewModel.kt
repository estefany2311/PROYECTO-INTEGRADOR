package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.audio.GrabadoraWav
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.model.audio.IGrabadora
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

// T-314: lo que muestra la parte de grabación
sealed class GrabacionUiState {
    object Lista : GrabacionUiState()                                        // todavía no graba
    data class Grabando(val duracionMs: Long, val nivelVoz: Float) : GrabacionUiState()
    data class Grabada(val grabacion: Grabacion) : GrabacionUiState()
    data class Error(val mensaje: String) : GrabacionUiState()
}

class GrabacionViewModel(
    private val grabadora: IGrabadora = GrabadoraWav(),
    private val carpeta: File = Dependencias.carpetaGrabaciones
) : ViewModel() {

    private val _uiState = MutableStateFlow<GrabacionUiState>(GrabacionUiState.Lista)
    val uiState: StateFlow<GrabacionUiState> = _uiState

    private var reloj: Job? = null

    fun grabar(nivelId: Int) {
        if (grabadora.grabando) return
        try {
            // Una grabación por nivel: si vuelve a grabar, reemplaza la anterior (T-317 decide cuánto se guarda)
            grabadora.iniciar(File(carpeta, "nivel_$nivelId.wav"))
        } catch (e: Exception) {
            _uiState.value = GrabacionUiState.Error(MENSAJE_SIN_MICROFONO)
            return
        }
        _uiState.value = GrabacionUiState.Grabando(0, 0f)
        // Cada 0,1 s se actualizan el tiempo y el volumen (los usa el indicador de T-315)
        reloj = viewModelScope.launch {
            while (isActive && grabadora.grabando) {
                _uiState.value = GrabacionUiState.Grabando(grabadora.duracionMs, grabadora.nivelVoz)
                delay(INTERVALO_MS)
            }
        }
    }

    fun terminar() {
        reloj?.cancel()
        val grabacion = grabadora.detener()
        _uiState.value = if (grabacion != null && grabacion.duracionMs > 0) GrabacionUiState.Grabada(grabacion)
        else GrabacionUiState.Error(MENSAJE_SIN_AUDIO)
    }

    fun grabarOtraVez() {
        _uiState.value = GrabacionUiState.Lista
    }

    // Si el niño sale de la pantalla mientras graba, se apaga el micrófono
    override fun onCleared() {
        reloj?.cancel()
        grabadora.detener()
    }

    companion object {
        const val INTERVALO_MS = 100L
        // T-315: tiempo máximo de una lectura (lo usa la barra de tiempo; T-316 detiene la grabación ahí)
        const val DURACION_MAXIMA_MS = 120_000L
        const val MENSAJE_SIN_MICROFONO = "No pudimos usar el micrófono. Pide ayuda a tu profesor."
        const val MENSAJE_SIN_AUDIO = "No escuchamos nada. Intenta grabar otra vez."
    }
}
