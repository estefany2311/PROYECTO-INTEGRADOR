package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.audio.AlmacenGrabaciones
import com.proyecto.movil.model.audio.GrabadoraWav
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.model.audio.IGrabadora
import com.proyecto.movil.model.audio.IReproductor
import com.proyecto.movil.model.audio.ReproductorAudio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// T-314: lo que muestra la parte de grabación
sealed class GrabacionUiState {
    object Lista : GrabacionUiState()                                        // todavía no graba
    data class Grabando(val duracionMs: Long, val nivelVoz: Float) : GrabacionUiState()
    // T-316: porTiempo = true cuando se detuvo sola al llegar a los 2 minutos
    // T-317: deAntes = true cuando es una grabación que ya estaba guardada en la tablet
    data class Grabada(
        val grabacion: Grabacion,
        val porTiempo: Boolean = false,
        val deAntes: Boolean = false
    ) : GrabacionUiState()
    data class Error(val mensaje: String) : GrabacionUiState()
}

// T-407: estado del reproductor de la grabación
data class Reproduccion(
    val reproduciendo: Boolean = false,
    val posicionMs: Long = 0,
    val error: String? = null
)

class GrabacionViewModel(
    private val grabadora: IGrabadora = GrabadoraWav(),
    private val almacen: AlmacenGrabaciones = Dependencias.almacenGrabaciones,
    // T-317: cada niño tiene sus grabaciones (varios usan la misma tablet)
    private val estudianteId: () -> Long = { Dependencias.almacenSesion.leer()?.estudianteId ?: 0L },
    private val reproductor: IReproductor = ReproductorAudio()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GrabacionUiState>(GrabacionUiState.Lista)
    val uiState: StateFlow<GrabacionUiState> = _uiState

    private val _reproduccion = MutableStateFlow(Reproduccion())
    val reproduccion: StateFlow<Reproduccion> = _reproduccion

    private var reloj: Job? = null
    private var avance: Job? = null
    private var nivelActual = 0

    // T-317: al abrir la pantalla, si ya grabó este nivel se le muestra su grabación
    fun revisarGuardada(nivelId: Int) {
        if (_uiState.value !is GrabacionUiState.Lista) return
        almacen.guardada(estudianteId(), nivelId)?.let {
            _uiState.value = GrabacionUiState.Grabada(it, deAntes = true)
        }
    }

    fun grabar(nivelId: Int) {
        if (grabadora.grabando) return
        nivelActual = nivelId
        try {
            // Se graba en un archivo aparte; la grabación anterior se reemplaza solo si esta sale bien (T-317)
            grabadora.iniciar(almacen.archivoEnCurso(estudianteId(), nivelId))
        } catch (e: Exception) {
            _uiState.value = GrabacionUiState.Error(MENSAJE_SIN_MICROFONO)
            return
        }
        _uiState.value = GrabacionUiState.Grabando(0, 0f)
        // Cada 0,1 s se actualizan el tiempo y el volumen (los usa el indicador de T-315)
        reloj = viewModelScope.launch {
            while (isActive && grabadora.grabando) {
                val duracion = grabadora.duracionMs
                // T-316: al llegar a los 2 minutos se detiene sola, así el audio no crece sin control
                if (duracion >= DURACION_MAXIMA_MS) {
                    detenerGrabacion(porTiempo = true)
                    break
                }
                _uiState.value = GrabacionUiState.Grabando(duracion, grabadora.nivelVoz)
                delay(INTERVALO_MS)
            }
        }
    }

    // El niño toca "Terminar"
    fun terminar() {
        reloj?.cancel()
        detenerGrabacion(porTiempo = false)
    }

    private fun detenerGrabacion(porTiempo: Boolean) {
        val grabacion = grabadora.detener()
        _uiState.value = if (grabacion != null && grabacion.duracionMs > 0) {
            // T-317: queda guardada en la tablet hasta que se envíe (US-13)
            GrabacionUiState.Grabada(almacen.guardar(estudianteId(), nivelActual, grabacion), porTiempo)
        } else {
            almacen.descartar(estudianteId(), nivelActual)
            GrabacionUiState.Error(MENSAJE_SIN_AUDIO)
        }
    }

    // Vuelve al botón de grabar. La grabación guardada sigue ahí hasta que una nueva salga bien.
    fun grabarOtraVez() {
        _uiState.value = GrabacionUiState.Lista
    }

    // T-407: el mismo botón sirve para escuchar y para pausar
    fun escuchar() {
        val grabada = _uiState.value as? GrabacionUiState.Grabada ?: return
        if (reproductor.reproduciendo) {
            pausar()
            return
        }
        try {
            if (!reproductor.preparado) reproductor.preparar(grabada.grabacion.archivo, ::alTerminarDeEscuchar)
            reproductor.reproducir()
        } catch (e: Exception) {
            liberarReproductor()
            _reproduccion.value = Reproduccion(error = MENSAJE_NO_SE_ESCUCHA)
            return
        }
        _reproduccion.value = Reproduccion(reproduciendo = true, posicionMs = reproductor.posicionMs)
        avance = viewModelScope.launch {
            while (isActive && reproductor.reproduciendo) {
                _reproduccion.value = Reproduccion(reproduciendo = true, posicionMs = reproductor.posicionMs)
                delay(INTERVALO_MS)
            }
        }
    }

    // Pausa donde iba
    fun pausar() {
        avance?.cancel()
        reproductor.pausar()
        if (_reproduccion.value.reproduciendo) {
            _reproduccion.value = Reproduccion(reproduciendo = false, posicionMs = reproductor.posicionMs)
        }
    }

    private fun alTerminarDeEscuchar() {
        avance?.cancel()
        _reproduccion.value = Reproduccion()
    }

    private fun liberarReproductor() {
        avance?.cancel()
        reproductor.liberar()
        _reproduccion.value = Reproduccion()
    }

    // Si el niño sale de la pantalla mientras graba, se apaga el micrófono y se borra la grabación a medias
    override fun onCleared() {
        reloj?.cancel()
        if (grabadora.grabando) {
            grabadora.detener()
            almacen.descartar(estudianteId(), nivelActual)
        }
    }

    companion object {
        const val INTERVALO_MS = 100L
        // T-315/T-316: tiempo máximo de una lectura (decisión de T-327). La barra lo muestra y ahí se detiene sola
        const val DURACION_MAXIMA_MS = 120_000L
        // T-316: en los últimos 15 segundos se avisa cuánto queda
        const val AVISO_FINAL_MS = 15_000L
        const val MENSAJE_SIN_MICROFONO = "No pudimos usar el micrófono. Pide ayuda a tu profesor."
        const val MENSAJE_SIN_AUDIO = "No escuchamos nada. Intenta grabar otra vez."
        const val MENSAJE_NO_SE_ESCUCHA = "No pudimos reproducir tu grabación. Vuelve a grabar."
    }
}
