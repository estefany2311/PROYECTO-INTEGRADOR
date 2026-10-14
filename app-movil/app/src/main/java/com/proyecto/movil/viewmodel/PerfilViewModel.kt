package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PerfilesUiState {
    object Cargando : PerfilesUiState()
    data class Exito(val perfiles: List<Perfil>) : PerfilesUiState()
    data class Error(val mensaje: String) : PerfilesUiState()
}

class PerfilViewModel(
    private val repository: PerfilRepository = PerfilRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilesUiState>(PerfilesUiState.Cargando)
    val uiState: StateFlow<PerfilesUiState> = _uiState

    fun cargarPerfiles(codigoAula: String) {
        _uiState.value = PerfilesUiState.Cargando
        viewModelScope.launch {
            repository.obtenerPerfiles(codigoAula)
                .onSuccess { _uiState.value = PerfilesUiState.Exito(it) }
                .onFailure { _uiState.value = PerfilesUiState.Error(it.message ?: "Error de conexión") }
        }
    }
}