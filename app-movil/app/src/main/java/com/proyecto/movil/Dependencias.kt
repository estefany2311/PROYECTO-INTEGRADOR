package com.proyecto.movil

import android.content.Context
import com.proyecto.movil.model.audio.AlmacenGrabaciones
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.model.local.AlmacenSesionSeguro
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.remote.IMonitorRed
import com.proyecto.movil.model.remote.MonitorRed
import com.proyecto.movil.model.remote.RetrofitClient

// Objetos compartidos por toda la app. Se inicializa una sola vez en AppMovil.onCreate().
object Dependencias {

    lateinit var almacenSesion: IAlmacenSesion
        private set

    // T-317: grabaciones de voz guardadas en la tablet hasta enviarlas (carpeta privada de la app)
    lateinit var almacenGrabaciones: AlmacenGrabaciones
        private set

    // T-325: borradores del editor de escritura guardados en la tablet
    lateinit var almacenBorradores: AlmacenBorradores
        private set

    // T-332: si la tablet tiene internet
    lateinit var monitorRed: IMonitorRed
        private set

    fun iniciar(context: Context) {
        if (::almacenSesion.isInitialized) return
        almacenSesion = AlmacenSesionSeguro(context.applicationContext)
        almacenGrabaciones = AlmacenGrabaciones(java.io.File(context.filesDir, "grabaciones"))
        // Borra grabaciones a medias y las de más de 7 días
        almacenGrabaciones.limpiar()
        almacenBorradores = AlmacenBorradores(java.io.File(context.filesDir, "borradores"))
        almacenBorradores.limpiar()   // borra los de más de 30 días
        monitorRed = MonitorRed(context.applicationContext)
        // T-314 grababa en la caché; esa carpeta ya no se usa
        java.io.File(context.cacheDir, "grabaciones").deleteRecursively()
        // Cada petición a la API lleva el token guardado (si hay sesión)
        RetrofitClient.proveedorToken = { almacenSesion.leer()?.token }
    }
}
