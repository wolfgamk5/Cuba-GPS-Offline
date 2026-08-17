package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa si el teléfono tiene una conexión a internet utilizable ahora mismo (WiFi o datos
 * móviles con validación de internet real, no solo "conectado a una red"). Se usa para decidir
 * cuándo la app puede complementar los datos offline con mapa/rutas en línea (Fase 4).
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** true mientras haya al menos una red con acceso a internet validado. */
    val isOnline: Flow<Boolean> = callbackFlow {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val activeNetworks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetworks.add(network)
                trySend(activeNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                activeNetworks.remove(network)
                trySend(activeNetworks.isNotEmpty())
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        // Estado inicial: por si ya había una red activa antes de registrar el callback.
        trySend(connectivityManager.activeNetwork != null)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
