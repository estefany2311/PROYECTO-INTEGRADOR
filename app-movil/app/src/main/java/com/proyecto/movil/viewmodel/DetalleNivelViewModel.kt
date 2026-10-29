package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.NivelDetalle
import com.proyecto.movil.model.ResultadoNivel
import com.proyecto.movil.model.repository.INivelRepository
import com.proyecto.movil.model.repository.NivelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// T-215: lo que muestra la pantalla de detalle del nivel
sealed class DetalleNivelUiState {
    object Cargando : DetalleNivelUiState()
    data class Exito(val detalle: NivelDetalle) : DetalleNivelUiState()
    // El backend no deja abrirlo (por ejemplo, si el mapa estaba desactualizado)
    data class Bloqueado(val mensaje: String) : DetalleNivelUiState()
    data class Error(val mensaje: String) : DetalleNivelUiState()
}

class DetalleNivelViewModel(
    private val repository: INivelRepository = NivelRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleNivelUiState>(DetalleNivelUiState.Cargando)
    val uiState: StateFlow<DetalleNivelUiState> = _uiState

    private var nivelId: Int? = null

    fun cargar(nivelId: Int) {
        this.nivelId = nivelId
        _uiState.value = DetalleNivelUiState.Cargando
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.obtenerDetalle(nivelId)) {
                is ResultadoNivel.Exito -> DetalleNivelUiState.Exito(resultado.detalle)
                is ResultadoNivel.Bloqueado ->
                    DetalleNivelUiState.Bloqueado(MapaViewModel.mensajeNivelBloqueado(resultado.nivelAnterior))
                ResultadoNivel.SesionVencida -> DetalleNivelUiState.Error(MENSAJE_SESION_VENCIDA)
                ResultadoNivel.NoEncontrado -> DetalleNivelUiState.Error(MENSAJE_NO_ENCONTRADO)
                ResultadoNivel.ErrorConexion -> DetalleNivelUiState.Error(MENSAJE_SIN_CONEXION)
            }
        }
    }

    fun reintentar() {
        nivelId?.let { cargar(it) }
    }

    companion object {
        const val MENSAJE_SIN_CONEXION = "No pudimos abrir este nivel. Intenta de nuevo."
        const val MENSAJE_NO_ENCONTRADO = "Este nivel ya no está en el mapa."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Pide ayuda para volver a entrar."
    }
}
