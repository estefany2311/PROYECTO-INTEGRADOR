package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.MapaRespuesta
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
    data class Exito(val mapa: MapaRespuesta) : MapaUiState()
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

    // T-210: pide al backend docente los niveles del alumno que tiene la sesión.
    // El backend sabe quién es por el token; sin sesión guardada no hay a quién pedirle el mapa.
    fun cargarMapa() {
        if (sesion == null) {
            _uiState.value = MapaUiState.Error(MENSAJE_ERROR)
            return
        }
        _uiState.value = MapaUiState.Cargando
        viewModelScope.launch {
            repository.obtenerMapa()
                .onSuccess { _uiState.value = MapaUiState.Exito(it) }
                .onFailure { _uiState.value = MapaUiState.Error(MENSAJE_ERROR) }
        }
    }

    /**
     * T-208: borra el token del dispositivo y devuelve el código del aula,
     * para volver a "¿Quién eres?" del mismo salón y que entre otro compañero.
     * T-236: antes avisa al backend para que el token deje de servir. Si no hay conexión
     * se espera como máximo [ESPERA_CIERRE_MS]: el niño nunca se queda atrapado en su sesión.
     */
    fun salir(alTerminar: (codigoAula: String?) -> Unit) {
        viewModelScope.launch {
            // Primero el backend: el interceptor todavía encuentra el token guardado
            withTimeoutOrNull(ESPERA_CIERRE_MS) { sesionRepository.cerrarSesion() }
            almacen.borrar()
            alTerminar(almacen.leerCodigoAula())
        }
    }

    companion object {
        // Mensaje para el niño: sin códigos ni palabras técnicas
        const val MENSAJE_ERROR = "No pudimos abrir tu mapa. Intenta de nuevo."

        // T-236: lo máximo que "Salir" espera al backend antes de cerrar igual en la tablet
        const val ESPERA_CIERRE_MS = 3_000L
    }
}
