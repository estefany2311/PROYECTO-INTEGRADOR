package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.model.ResultadoOrtografia
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.model.remote.IMonitorRed
import com.proyecto.movil.model.repository.IOrtografiaRepository
import com.proyecto.movil.model.repository.OrtografiaRepository
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
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val ortografia: IOrtografiaRepository = OrtografiaRepository(),
    private val red: IMonitorRed = Dependencias.monitorRed
) : ViewModel() {

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto

    private val _guardado = MutableStateFlow(EstadoGuardado.NADA)
    val guardado: StateFlow<EstadoGuardado> = _guardado

    // T-330: errores de ortografía del texto actual (T-331 los subraya)
    private val _errores = MutableStateFlow<List<ErrorOrtografico>>(emptyList())
    val errores: StateFlow<List<ErrorOrtografico>> = _errores

    private var nivelId: Int? = null
    private var verificacion: Job? = null

    // T-332: true cuando no se pudo revisar por falta de internet; se revisa sola al volver la conexión
    private val _sinRevisar = MutableStateFlow(false)
    val sinRevisar: StateFlow<Boolean> = _sinRevisar

    init {
        viewModelScope.launch {
            red.conectado.collect { conectado ->
                if (conectado && _sinRevisar.value) programarVerificacion(espera = 0L)
            }
        }
    }
    private var guardadoPendiente: Job? = null
    private var sinGuardar = false

    // Al abrir el editor: si había un borrador de este nivel, se recupera
    fun abrir(nivelId: Int) {
        if (this.nivelId == nivelId) return          // al girar la pantalla no se vuelve a leer
        this.nivelId = nivelId
        almacen.leer(estudianteId(), nivelId)?.let {
            _texto.value = it
            _guardado.value = EstadoGuardado.GUARDADO
            programarVerificacion()
        }
    }

    fun escribir(nuevo: String) {
        // Un límite generoso para que un dedo apoyado en una tecla no llene la memoria
        val texto = nuevo.take(MAX_CARACTERES)
        if (texto == _texto.value) return
        _texto.value = texto
        sinGuardar = true
        programarGuardado()
        conservarErroresQueSiguen(texto)
        programarVerificacion()
    }

    // T-330: se verifica cuando el niño deja de escribir un momento, no con cada letra
    private fun programarVerificacion(espera: Long = ESPERA_VERIFICAR_MS) {
        verificacion?.cancel()
        if (_texto.value.isBlank()) {
            _errores.value = emptyList()
            _sinRevisar.value = false
            return
        }
        verificacion = viewModelScope.launch {
            delay(espera)
            // T-332: sin internet ni se intenta; queda pendiente hasta que vuelva
            if (!red.conectado.value) {
                _sinRevisar.value = true
                return@launch
            }
            val enviado = _texto.value
            when (val resultado = ortografia.verificar(enviado)) {
                is ResultadoOrtografia.Exito -> {
                    // Si siguió escribiendo mientras llegaba la respuesta, esas posiciones ya no sirven
                    if (enviado == _texto.value) {
                        _errores.value = resultado.errores
                        _sinRevisar.value = false
                    }
                }
                ResultadoOrtografia.SinConexion -> _sinRevisar.value = true
                ResultadoOrtografia.SesionVencida -> Unit
            }
        }
    }

    // Mientras llega la nueva revisión, se dejan solo los errores cuya palabra sigue en el mismo lugar
    private fun conservarErroresQueSiguen(texto: String) {
        _errores.value = _errores.value.filter { e ->
            e.fin <= texto.length && texto.substring(e.inicio, e.fin) == e.palabra
        }
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
            if (texto == _texto.value) sinGuardar = false
            _guardado.value = if (texto.isBlank()) EstadoGuardado.NADA else EstadoGuardado.GUARDADO
        }
    }

    // Al salir del editor se guarda en ese momento lo que faltaba (el archivo es pequeño).
    // Se usa sinGuardar porque Android cancela viewModelScope antes de llamar a onCleared()
    override fun onCleared() {
        val nivel = nivelId ?: return
        if (sinGuardar) almacen.guardar(estudianteId(), nivel, _texto.value)
    }

    companion object {
        const val MAX_CARACTERES = 3_000
        const val ESPERA_GUARDADO_MS = 1_000L
        const val ESPERA_VERIFICAR_MS = 800L
    }
}
