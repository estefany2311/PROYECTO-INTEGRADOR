package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.ISesionRepository
import com.proyecto.movil.model.repository.SesionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// T-207: el tipo de error decide el ícono y el color del aviso
enum class TipoError { PIN_INCORRECTO, BLOQUEADO, SIN_CONEXION }

data class PinUiState(
    val pin: String = "",               // nunca se muestra, solo se cuentan los círculos rellenos
    val verificando: Boolean = false,
    val mensajeError: String? = null,
    val sesion: SesionRespuesta? = null,
    val detalleError: String? = null,   // T-207: "Te quedan 3 intentos." o el tiempo de espera
    val tipoError: TipoError? = null,   // T-207: para el ícono y el color
    val sacudidas: Int = 0,             // T-234: sube en cada error; la pantalla sacude los círculos al cambiar
    val sacudiendo: Boolean = false     // T-234: mientras dura la sacudida el teclado espera
)

class PinViewModel(
    private val repository: ISesionRepository = SesionRepository(),
    private val almacen: IAlmacenSesion = Dependencias.almacenSesion
) : ViewModel() {

    companion object {
        const val LARGO_PIN = 4
        const val MENSAJE_PIN_INCORRECTO = "Esa no es tu clave. Inténtalo otra vez."
        const val MENSAJE_BLOQUEADO = "Espera un momento y vuelve a intentarlo."
        const val MENSAJE_SIN_CONEXION = "No pudimos entrar. Pídele ayuda a tu profesor."

        // T-234: lo que dura la sacudida; después se borran los 4 círculos solos
        const val DURACION_SACUDIDA_MS = 600L

        // T-207: textos con los datos que manda el backend
        fun textoIntentos(intentosRestantes: Int?): String? = when (intentosRestantes) {
            null -> null
            1 -> "Te queda 1 intento."
            else -> "Te quedan $intentosRestantes intentos."
        }

        fun textoBloqueo(segundosRestantes: Long?): String {
            if (segundosRestantes == null) return "Podrás intentar otra vez en un momento."
            val minutos = (segundosRestantes + 59) / 60     // redondea hacia arriba: 61 s = 2 minutos
            return if (minutos <= 1) "Podrás intentar otra vez en 1 minuto."
            else "Podrás intentar otra vez en $minutos minutos."
        }
    }

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState

    private var estudianteId: Long = 0

    // Perfil elegido en "¿Quién eres?"
    fun inicializar(estudianteId: Long) {
        this.estudianteId = estudianteId
    }

    // Al completar los 4 números se verifica solo, sin botón de "Entrar"
    fun presionarDigito(digito: Int) {
        val estado = _uiState.value
        if (estado.verificando || estado.sacudiendo || estado.sesion != null ||
            digito !in 0..9 || estado.pin.length >= LARGO_PIN) return

        val nuevoPin = estado.pin + digito
        _uiState.value = estado.copy(pin = nuevoPin)
        if (nuevoPin.length == LARGO_PIN) verificar(nuevoPin)
    }

    fun borrar() {
        val estado = _uiState.value
        if (estado.verificando || estado.sacudiendo || estado.sesion != null) return
        _uiState.value = estado.copy(pin = estado.pin.dropLast(1))
    }

    private fun verificar(pin: String) {
        // Al verificar se quita el aviso del intento anterior
        _uiState.update { it.copy(verificando = true, mensajeError = null, detalleError = null, tipoError = null) }
        viewModelScope.launch {
            when (val resultado = repository.iniciarSesion(estudianteId, pin)) {
                is ResultadoLogin.Exito -> {
                    val sesion = resultado.sesion
                    // T-205: primero se guarda el token cifrado y después se avisa a la pantalla
                    almacen.guardar(
                        SesionGuardada(sesion.token, sesion.estudianteId, sesion.nombre, sesion.avatarUrl)
                    )
                    _uiState.update { it.copy(verificando = false, sesion = sesion) }
                }
                is ResultadoLogin.PinIncorrecto -> fallarConSacudida(
                    MENSAJE_PIN_INCORRECTO, textoIntentos(resultado.intentosRestantes), TipoError.PIN_INCORRECTO
                )
                is ResultadoLogin.Bloqueado -> fallarConSacudida(
                    MENSAJE_BLOQUEADO, textoBloqueo(resultado.segundosRestantes), TipoError.BLOQUEADO
                )
                // Sin conexión no es culpa del niño: no se sacude, solo se limpia
                is ResultadoLogin.ErrorConexion -> fallar(
                    MENSAJE_SIN_CONEXION, null, TipoError.SIN_CONEXION
                )
            }
        }
    }

    /**
     * T-234: los 4 círculos se quedan llenos mientras se sacuden (así el niño ve que se equivocó)
     * y después se borran solos para que escriba de nuevo desde cero.
     */
    private suspend fun fallarConSacudida(mensaje: String, detalle: String?, tipo: TipoError) {
        _uiState.update {
            it.copy(
                verificando = false, mensajeError = mensaje, detalleError = detalle, tipoError = tipo,
                sacudidas = it.sacudidas + 1, sacudiendo = true
            )
        }
        delay(DURACION_SACUDIDA_MS)
        _uiState.update { it.copy(pin = "", sacudiendo = false) }
    }

    // Se limpia el PIN para que el estudiante lo escriba de nuevo desde cero
    private fun fallar(mensaje: String, detalle: String?, tipo: TipoError) {
        _uiState.update {
            it.copy(pin = "", verificando = false, mensajeError = mensaje, detalleError = detalle, tipoError = tipo)
        }
    }
}