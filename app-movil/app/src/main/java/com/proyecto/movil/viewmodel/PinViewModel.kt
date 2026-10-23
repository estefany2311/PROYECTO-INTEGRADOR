package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.model.repository.ISesionRepository
import com.proyecto.movil.model.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada

data class PinUiState(
    val pin: String = "",               // nunca se muestra, solo se cuentan los círculos rellenos
    val verificando: Boolean = false,
    val mensajeError: String? = null,
    val sesion: SesionRespuesta? = null
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
        if (estado.verificando || estado.sesion != null || digito !in 0..9 || estado.pin.length >= LARGO_PIN) return

        val nuevoPin = estado.pin + digito
        _uiState.value = estado.copy(pin = nuevoPin)
        if (nuevoPin.length == LARGO_PIN) verificar(nuevoPin)
    }

    fun borrar() {
        val estado = _uiState.value
        if (estado.verificando || estado.sesion != null) return
        _uiState.value = estado.copy(pin = estado.pin.dropLast(1))
    }

    private fun verificar(pin: String) {
        _uiState.update { it.copy(verificando = true, mensajeError = null) }
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
                is ResultadoLogin.PinIncorrecto -> fallar(MENSAJE_PIN_INCORRECTO)
                is ResultadoLogin.Bloqueado -> fallar(MENSAJE_BLOQUEADO)
                is ResultadoLogin.ErrorConexion -> fallar(MENSAJE_SIN_CONEXION)
            }
        }
    }

    // Se limpia el PIN para que el estudiante lo escriba de nuevo desde cero
    private fun fallar(mensaje: String) {
        _uiState.update { it.copy(pin = "", verificando = false, mensajeError = mensaje) }
    }
}
