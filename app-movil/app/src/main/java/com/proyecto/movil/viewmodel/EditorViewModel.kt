package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.model.ResultadoEnvioTexto
import com.proyecto.movil.model.ResultadoEscritura
import com.proyecto.movil.model.ResultadoOrtografia
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.model.reemplazarPalabra
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.model.remote.IMonitorRed
import com.proyecto.movil.model.repository.IOrtografiaRepository
import com.proyecto.movil.model.repository.ITextoRepository
import com.proyecto.movil.model.repository.OrtografiaRepository
import com.proyecto.movil.model.repository.TextoRepository
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

// T-435 (US-21): el envío del texto final
sealed class EnvioTextoUiState {
    object Escribiendo : EnvioTextoUiState()
    object Confirmando : EnvioTextoUiState()     // "¿Enviar tu texto?"
    object Enviando : EnvioTextoUiState()
    data class Enviado(val escritura: ResultadoEscritura?, val reto: ResultadoReto?) : EnvioTextoUiState()
    data class Error(val mensaje: String) : EnvioTextoUiState()
    data class YaEnviado(val texto: String) : EnvioTextoUiState()   // T-436: lo envió antes; solo se puede ver
}

class EditorViewModel(
    private val almacen: AlmacenBorradores = Dependencias.almacenBorradores,
    private val estudianteId: () -> Long = { Dependencias.almacenSesion.leer()?.estudianteId ?: 0L },
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val ortografia: IOrtografiaRepository = OrtografiaRepository(),
    private val red: IMonitorRed = Dependencias.monitorRed,
    private val textos: ITextoRepository = TextoRepository()
) : ViewModel() {

    private val _envio = MutableStateFlow<EnvioTextoUiState>(EnvioTextoUiState.Escribiendo)
    val envio: StateFlow<EnvioTextoUiState> = _envio

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
        // T-436: si ya lo envió, se muestra su texto y no se puede volver a editar
        almacen.enviado(estudianteId(), nivelId)?.let {
            _texto.value = it
            _envio.value = EnvioTextoUiState.YaEnviado(it)
            return
        }
        almacen.leer(estudianteId(), nivelId)?.let {
            _texto.value = it
            _guardado.value = EstadoGuardado.GUARDADO
            programarVerificacion()
        }
    }

    fun escribir(nuevo: String) {
        if (bloqueado()) return                      // T-436: enviando o ya enviado
        // Un límite generoso para que un dedo apoyado en una tecla no llene la memoria
        val texto = nuevo.take(MAX_CARACTERES)
        if (texto == _texto.value) return
        _texto.value = texto
        sinGuardar = true
        programarGuardado()
        conservarErroresQueSiguen(texto)
        programarVerificacion()
    }

    // T-367 (US-20): cambia la palabra subrayada por la sugerencia que eligió en el menú (T-366)
    fun elegirSugerencia(error: ErrorOrtografico, sugerencia: String) {
        if (bloqueado()) return
        val nuevo = reemplazarPalabra(_texto.value, error, sugerencia) ?: return
        // Ese error ya se corrigió; los que siguen se corren lo que cambió el largo de la palabra
        val corrimiento = sugerencia.length - (error.fin - error.inicio)
        _errores.value = _errores.value
            .filter { it != error }
            .map { if (it.inicio >= error.fin) it.copy(inicio = it.inicio + corrimiento, fin = it.fin + corrimiento) else it }
        escribir(nuevo)   // se guarda en la tablet y se vuelve a revisar como cualquier cambio
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

    // T-435: el botón "Enviar" pide confirmar antes de enviar
    fun pedirEnviar() {
        if (_texto.value.isBlank()) return
        if (bloqueado()) return
        _envio.value = EnvioTextoUiState.Confirmando
    }

    fun cancelarEnvio() {
        if (_envio.value is EnvioTextoUiState.Confirmando) _envio.value = EnvioTextoUiState.Escribiendo
    }

    fun confirmarEnvio() {
        val nivel = nivelId ?: return
        if (_envio.value !is EnvioTextoUiState.Confirmando) return
        _envio.value = EnvioTextoUiState.Enviando
        val enviado = _texto.value
        viewModelScope.launch {
            _envio.value = when (val r = textos.enviar(nivel, enviado)) {
                is ResultadoEnvioTexto.Exito -> {
                    // Ya está en el servidor: se guarda como enviado (T-436) y se borra el borrador
                    guardadoPendiente?.cancel()
                    sinGuardar = false
                    withContext(io) { almacen.marcarEnviado(estudianteId(), nivel, enviado) }
                    EnvioTextoUiState.Enviado(r.escritura, r.reto)
                }
                is ResultadoEnvioTexto.Rechazado -> EnvioTextoUiState.Error(r.mensaje)
                ResultadoEnvioTexto.SinConexion -> EnvioTextoUiState.Error(MENSAJE_SIN_CONEXION)
                ResultadoEnvioTexto.SesionVencida -> EnvioTextoUiState.Error(MENSAJE_SESION_VENCIDA)
            }
        }
    }

    // T-436: mientras se envía y después de enviar, el texto no se puede cambiar
    private fun bloqueado(): Boolean = when (_envio.value) {
        is EnvioTextoUiState.Enviando, is EnvioTextoUiState.Enviado, is EnvioTextoUiState.YaEnviado -> true
        else -> false
    }

    // Al salir del editor se guarda en ese momento lo que faltaba (el archivo es pequeño).
    // Se usa sinGuardar porque Android cancela viewModelScope antes de llamar a onCleared()
    override fun onCleared() {
        val nivel = nivelId ?: return
        if (sinGuardar && !bloqueado()) almacen.guardar(estudianteId(), nivel, _texto.value)
    }

    companion object {
        const val MAX_CARACTERES = 3_000
        const val ESPERA_GUARDADO_MS = 1_000L
        const val ESPERA_VERIFICAR_MS = 800L
        const val MENSAJE_SIN_CONEXION = "No hay internet. Tu texto sigue guardado en la tablet; intenta enviarlo otra vez."
        const val MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Tu texto sigue guardado en la tablet."
    }
}
