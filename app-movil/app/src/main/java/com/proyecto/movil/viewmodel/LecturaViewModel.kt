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

    // T-311: tamaño de letra elegido por el niño (posición en TAMANOS_LETRA). Empieza en 32 sp.
    private val _indiceTamano = MutableStateFlow(INDICE_INICIAL)
    val indiceTamano: StateFlow<Int> = _indiceTamano

    fun agrandarLetra() {
        if (_indiceTamano.value < TAMANOS_LETRA.lastIndex) _indiceTamano.value++
    }

    fun achicarLetra() {
        if (_indiceTamano.value > 0) _indiceTamano.value--
    }

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
        // T-311: tamaños de letra disponibles (en sp): de cómodo para grados altos a muy grande para 1.º y 2.º
        val TAMANOS_LETRA = listOf(24, 28, 32, 38, 44)
        const val INDICE_INICIAL = 2      // 32 sp

        const val MENSAJE_SIN_LECTURA = "Tu profesor todavía no preparó una lectura para este nivel."
        const val MENSAJE_SIN_CONEXION = "No pudimos traer tu lectura. Intenta de nuevo."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Pide ayuda para volver a entrar."
    }
}
