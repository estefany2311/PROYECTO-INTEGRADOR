package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.ResultadoConsigna
import com.proyecto.movil.model.repository.ConsignaRepository
import com.proyecto.movil.model.repository.IConsignaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// T-321: lo que muestra la pantalla de la consigna
sealed class ConsignaUiState {
    object Cargando : ConsignaUiState()
    data class Exito(val consigna: ConsignaNivel) : ConsignaUiState()
    // puedeReintentar = false cuando reintentar no cambia nada (nivel bloqueado, sin consigna)
    data class Error(val mensaje: String, val puedeReintentar: Boolean) : ConsignaUiState()
}

class ConsignaViewModel(
    private val repository: IConsignaRepository = ConsignaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConsignaUiState>(ConsignaUiState.Cargando)
    val uiState: StateFlow<ConsignaUiState> = _uiState

    private var nivelId: Int? = null

    fun cargar(nivelId: Int) {
        this.nivelId = nivelId
        _uiState.value = ConsignaUiState.Cargando
        viewModelScope.launch {
            _uiState.value = when (val r = repository.obtenerConsigna(nivelId)) {
                is ResultadoConsigna.Exito -> ConsignaUiState.Exito(r.consigna)
                is ResultadoConsigna.Bloqueado ->
                    ConsignaUiState.Error(MapaViewModel.mensajeNivelBloqueado(r.nivelAnterior), puedeReintentar = false)
                ResultadoConsigna.SinConsigna -> ConsignaUiState.Error(MENSAJE_SIN_CONSIGNA, puedeReintentar = false)
                ResultadoConsigna.SesionVencida -> ConsignaUiState.Error(MENSAJE_SESION_VENCIDA, puedeReintentar = false)
                ResultadoConsigna.ErrorConexion -> ConsignaUiState.Error(MENSAJE_SIN_CONEXION, puedeReintentar = true)
            }
        }
    }

    fun reintentar() {
        nivelId?.let { cargar(it) }
    }

    companion object {
        const val MENSAJE_SIN_CONSIGNA = "Tu profesor todavía no preparó este reto de escritura."
        const val MENSAJE_SIN_CONEXION = "No pudimos traer tu reto. Intenta de nuevo."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Pide ayuda para volver a entrar."
    }
}
