package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.model.repository.IMensajeRepository
import com.proyecto.movil.model.repository.MensajeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Mensajes del profesor que se ven en la pestaña Logros.
// Si falla la red se quedan los que ya se habían cargado (o ninguno): no se muestra error al niño
class MensajesViewModel(
    private val repository: IMensajeRepository = MensajeRepository()
) : ViewModel() {

    private val _mensajes = MutableStateFlow<List<MensajeDocente>>(emptyList())
    val mensajes: StateFlow<List<MensajeDocente>> = _mensajes

    fun cargar() {
        viewModelScope.launch {
            repository.mensajes().onSuccess { _mensajes.value = it }
        }
    }
}
