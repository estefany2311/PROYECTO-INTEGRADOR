package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.IMapaRepository
import com.proyecto.movil.model.repository.ISesionRepository
import com.proyecto.movil.model.repository.MapaRepository
import com.proyecto.movil.model.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// T-210: lo que muestra la pantalla del mapa
sealed class MapaUiState {
    object Cargando : MapaUiState()
    // T-216: recienDesbloqueados = niveles que estaban bloqueados y se abrieron desde la última carga
    data class Exito(val mapa: MapaRespuesta, val recienDesbloqueados: Set<Int> = emptySet()) : MapaUiState()
    data class Error(val mensaje: String) : MapaUiState()
}

class MapaViewModel(
    private val almacen: IAlmacenSesion = Dependencias.almacenSesion,
    private val repository: IMapaRepository = MapaRepository(),
    private val sesionRepository: ISesionRepository = SesionRepository()
) : ViewModel() {

    // Quién tiene la sesión abierta (nombre y avatar para la barra de arriba)
    val sesion: SesionGuardada? get() = almacen.leer()

    private val _uiState = MutableStateFlow<MapaUiState>(MapaUiState.Cargando)
    val uiState: StateFlow<MapaUiState> = _uiState

    // T-240: mensaje que aparece al tocar un nivel bloqueado (null = no hay mensaje)
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso

    fun tocarNivel(nivel: NivelMapa) {
        if (!nivel.bloqueado) return
        val niveles = (_uiState.value as? MapaUiState.Exito)?.mapa?.niveles.orEmpty()
        // El nivel que va justo antes en el camino
        val anterior = niveles.filter { it.orden < nivel.orden }.maxByOrNull { it.orden }
        _aviso.value = mensajeNivelBloqueado(anterior?.nombre)
    }

    fun cerrarAviso() {
        _aviso.value = null
    }

    // T-210: pide al backend docente los niveles del alumno que tiene la sesión.
    // El backend sabe quién es por el token; sin sesión guardada no hay a quién pedirle el mapa.
    // T-216: al volver al mapa (por ejemplo, después de terminar un reto) se refresca sin mostrar
    // la ruedita: el mapa anterior sigue en pantalla y se compara con el nuevo para animar
    // los niveles que se acaban de desbloquear. Si el refresco falla, se queda el mapa que ya había.
    fun cargarMapa() {
        if (sesion == null) {
            _uiState.value = MapaUiState.Error(MENSAJE_ERROR)
            return
        }
        val anterior = (_uiState.value as? MapaUiState.Exito)?.mapa
        if (anterior == null) _uiState.value = MapaUiState.Cargando
        viewModelScope.launch {
            repository.obtenerMapa()
                .onSuccess { nuevo ->
                    _uiState.value = MapaUiState.Exito(nuevo, desbloqueadosEntre(anterior, nuevo))
                }
                .onFailure {
                    _uiState.value = if (anterior != null) MapaUiState.Exito(anterior) else MapaUiState.Error(MENSAJE_ERROR)
                }
        }
    }

    // T-216: la pantalla avisa que ya mostró la animación, para no repetirla al girar la tablet
    fun animacionDesbloqueoMostrada() {
        val estado = _uiState.value as? MapaUiState.Exito ?: return
        if (estado.recienDesbloqueados.isNotEmpty()) _uiState.value = estado.copy(recienDesbloqueados = emptySet())
    }

    fun salir(alTerminar: (codigoAula: String?) -> Unit) {
        viewModelScope.launch {
            // Primero el backend: el interceptor todavía encuentra el token guardado
            withTimeoutOrNull(ESPERA_CIERRE_MS) { sesionRepository.cerrarSesion() }
            almacen.borrar()
            alTerminar(almacen.leerCodigoAula())
        }
    }

    companion object {
        // T-216: ids de los niveles que estaban bloqueados en el mapa anterior y ya no lo están.
        // En la primera carga (sin mapa anterior) no se anima nada.
        fun desbloqueadosEntre(anterior: MapaRespuesta?, nuevo: MapaRespuesta): Set<Int> {
            if (anterior == null) return emptySet()
            val bloqueadosAntes = anterior.niveles.filter { it.bloqueado }.map { it.id }.toSet()
            return nuevo.niveles.filter { !it.bloqueado && it.id in bloqueadosAntes }.map { it.id }.toSet()
        }

        // Mensaje para el niño: sin códigos ni palabras técnicas
        const val MENSAJE_ERROR = "No pudimos abrir tu mapa. Intenta de nuevo."

        // T-236: lo máximo que "Salir" espera al backend antes de cerrar igual en la tablet
        const val ESPERA_CIERRE_MS = 3_000L

        // T-240: tiempo que se ve el mensaje del nivel bloqueado antes de ocultarse solo
        const val DURACION_AVISO_MS = 4_000L

        // T-240: texto amable, sin palabras como "error" o "prohibido"
        fun mensajeNivelBloqueado(nivelAnterior: String?): String =
            if (nivelAnterior == null) "¡Todavía no! Este nivel se abre pronto."
            else "¡Todavía no! Termina «$nivelAnterior» para abrir este nivel."
    }
}
