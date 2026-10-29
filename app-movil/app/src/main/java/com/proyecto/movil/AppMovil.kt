package com.proyecto.movil

import android.app.Application

// Se ejecuta antes que cualquier pantalla (registrada en el AndroidManifest)
class AppMovil : Application() {
    override fun onCreate() {
        super.onCreate()
        Dependencias.iniciar(this)
    }
}