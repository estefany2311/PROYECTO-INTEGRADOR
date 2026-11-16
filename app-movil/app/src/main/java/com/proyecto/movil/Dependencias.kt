package com.proyecto.movil

import android.content.Context
import com.proyecto.movil.model.local.AlmacenSesionSeguro
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.remote.RetrofitClient

// Objetos compartidos por toda la app. Se inicializa una sola vez en AppMovil.onCreate().
object Dependencias {

    lateinit var almacenSesion: IAlmacenSesion
        private set

    fun iniciar(context: Context) {
        if (::almacenSesion.isInitialized) return
        almacenSesion = AlmacenSesionSeguro(context.applicationContext)
        // Cada petición a la API lleva el token guardado (si hay sesión)
        RetrofitClient.proveedorToken = { almacenSesion.leer()?.token }
    }
}