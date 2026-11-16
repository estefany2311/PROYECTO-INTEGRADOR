package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.ISesionRepository

// Repositorio falso: el PIN correcto es 1234; cualquier otro devuelve el resultado configurado
class SesionRepositorioFalso(
    private val alFallar: ResultadoLogin = ResultadoLogin.PinIncorrecto(intentosRestantes = 4)
) : ISesionRepository {
    val pinesRecibidos = mutableListOf<String>()

    override suspend fun iniciarSesion(estudianteId: Long, pin: String): ResultadoLogin {
        pinesRecibidos += pin
        return if (pin == "1234") {
            ResultadoLogin.Exito(SesionRespuesta("token-123", estudianteId, "Ana", "https://avatar/ana.png"))
        } else {
            alFallar
        }
    }
}

// Almacén en memoria en lugar del Keystore (que solo existe en un dispositivo real)
class AlmacenSesionFalso : IAlmacenSesion {
    var sesion: SesionGuardada? = null
    var codigoAula: String? = null

    override fun guardar(sesion: SesionGuardada) { this.sesion = sesion }
    override fun leer(): SesionGuardada? = sesion
    override fun borrar() { sesion = null }
    override fun guardarCodigoAula(codigo: String) { codigoAula = codigo }
    override fun leerCodigoAula(): String? = codigoAula
}