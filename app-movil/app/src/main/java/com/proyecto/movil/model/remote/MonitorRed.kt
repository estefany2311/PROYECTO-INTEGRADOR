package com.proyecto.movil.model.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// T-332 (US-19): avisa si la tablet tiene internet; en las pruebas se usa una versión falsa
interface IMonitorRed {
    val conectado: StateFlow<Boolean>
}

class MonitorRed(context: Context) : IMonitorRed {

    private val conectividad = context.getSystemService(ConnectivityManager::class.java)
    private val _conectado = MutableStateFlow(hayInternet())
    override val conectado: StateFlow<Boolean> = _conectado

    init {
        conectividad.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _conectado.value = true }
            override fun onLost(network: Network) { _conectado.value = hayInternet() }
        })
    }

    private fun hayInternet(): Boolean {
        val capacidades = conectividad.getNetworkCapabilities(conectividad.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
