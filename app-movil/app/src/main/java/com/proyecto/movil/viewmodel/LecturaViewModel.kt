package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.model.ResultadoLectura
import com.proyecto.movil.model.repository.ILecturaRepository
import com.proyecto.movil.model.repository.LecturaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// T-219: lo que muestra la pantalla de lectura
sealed class LecturaUiState {
    object Cargando : LecturaUiState()
    data class Exito(val lectura: LecturaNivel) : LecturaUiState()
    // puedeReintentar = false cuando reintentar no cambia nada (nivel bloqueado, sin lectura)
    data class Error(val mensaje: String, val puedeReintentar: Boolean) : LecturaUiState()
}

class LecturaViewModel(
    private val repository: ILecturaRepository = LecturaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LecturaUiState>(LecturaUiState.Cargando)
    val uiState: StateFlow<LecturaUiState> = _uiState

    private var nivelId: Int? = null

    fun cargar(nivelId: Int) {
        this.nivelId = nivelId
        _uiState.value = LecturaUiState.Cargando
        viewModelScope.launch {
            _uiState.value = when (val r = repository.obtenerLectura(nivelId)) {
                is ResultadoLectura.Exito -> LecturaUiState.Exito(r.lectura)
                is ResultadoLectura.Bloqueado ->
                    LecturaUiState.Error(MapaViewModel.mensajeNivelBloqueado(r.nivelAnterior), puedeReintentar = false)
                ResultadoLectura.SinLectura -> LecturaUiState.Error(MENSAJE_SIN_LECTURA, puedeReintentar = false)
                ResultadoLectura.SesionVencida -> LecturaUiState.Error(MENSAJE_SESION_VENCIDA, puedeReintentar = false)
                ResultadoLectura.ErrorConexion -> LecturaUiState.Error(MENSAJE_SIN_CONEXION, puedeReintentar = true)
            }
        }
    }

    fun reintentar() {
        nivelId?.let { cargar(it) }
    }

    companion object {
        const val MENSAJE_SIN_LECTURA = "Tu profesor todavía no preparó una lectura para este nivel."
        const val MENSAJE_SIN_CONEXION = "No pudimos traer tu lectura. Intenta de nuevo."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Pide ayuda para volver a entrar."
    }
}
