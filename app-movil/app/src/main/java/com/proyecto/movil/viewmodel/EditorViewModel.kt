package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.local.AlmacenBorradores
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// T-325: lo que muestra el editor junto al contador ("Guardando…" / "Guardado en la tablet")
enum class EstadoGuardado { NADA, GUARDANDO, GUARDADO }

class EditorViewModel(
    private val almacen: AlmacenBorradores = Dependencias.almacenBorradores,
    private val estudianteId: () -> Long = { Dependencias.almacenSesion.leer()?.estudianteId ?: 0L },
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto

    private val _guardado = MutableStateFlow(EstadoGuardado.NADA)
    val guardado: StateFlow<EstadoGuardado> = _guardado

    private var nivelId: Int? = null
    private var guardadoPendiente: Job? = null

    // Al abrir el editor: si había un borrador de este nivel, se recupera
    fun abrir(nivelId: Int) {
        if (this.nivelId == nivelId) return          // al girar la pantalla no se vuelve a leer
        this.nivelId = nivelId
        almacen.leer(estudianteId(), nivelId)?.let {
            _texto.value = it
            _guardado.value = EstadoGuardado.GUARDADO
        }
    }

    fun escribir(nuevo: String) {
        // Un límite generoso para que un dedo apoyado en una tecla no llene la memoria
        val texto = nuevo.take(MAX_CARACTERES)
        if (texto == _texto.value) return
        _texto.value = texto
        programarGuardado()
    }

    // Espera a que deje de escribir un momento; así no se escribe en la tablet con cada letra
    private fun programarGuardado() {
        val nivel = nivelId ?: return
        guardadoPendiente?.cancel()
        _guardado.value = EstadoGuardado.GUARDANDO
        guardadoPendiente = viewModelScope.launch {
            delay(ESPERA_GUARDADO_MS)
            val texto = _texto.value
            withContext(io) { almacen.guardar(estudianteId(), nivel, texto) }
            _guardado.value = if (texto.isBlank()) EstadoGuardado.NADA else EstadoGuardado.GUARDADO
        }
    }

    // Al salir del editor se guarda en ese momento lo que faltaba (el archivo es pequeño)
    override fun onCleared() {
        val nivel = nivelId ?: return
        if (guardadoPendiente?.isActive == true) {
            guardadoPendiente?.cancel()
            almacen.guardar(estudianteId(), nivel, _texto.value)
        }
    }

    companion object {
        const val MAX_CARACTERES = 3_000
        const val ESPERA_GUARDADO_MS = 1_000L
    }
}
