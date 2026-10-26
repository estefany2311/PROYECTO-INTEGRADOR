package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada
import kotlinx.coroutines.launch

class MapaViewModel(
    private val almacen: IAlmacenSesion = Dependencias.almacenSesion
) : ViewModel() {

    // Quién tiene la sesión abierta (nombre y avatar para la barra de arriba)
    val sesion: SesionGuardada? get() = almacen.leer()

    /**
     * T-208: borra el token del dispositivo y devuelve el código del aula,
     * para volver a "¿Quién eres?" del mismo salón y que entre otro compañero.
     * Se usa una corrutina porque en T-236 aquí se avisará también al backend.
     */
    fun salir(alTerminar: (codigoAula: String?) -> Unit) {
        viewModelScope.launch {
            almacen.borrar()
            alTerminar(almacen.leerCodigoAula())
        }
    }
}