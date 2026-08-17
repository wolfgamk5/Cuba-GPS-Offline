package com.example.maps

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.location.GpsLocationData
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.io.File

/**
 * Modo de visualización del mapa. NORMAL_2D = vista cenital clásica.
 * FREE_3D = vista inclinada libre (explorar el mapa en 3D con edificios).
 * NAVIGATION_FPV = primera persona siguiendo al usuario durante la navegación activa.
 */
enum class CubaMapViewMode { NORMAL_2D, FREE_3D, NAVIGATION_FPV }

/**
 * De dónde sale el mapa que se está pintando ahora mismo.
 * LOCAL = cuba.mbtiles descargado, 100% offline, con edificios en 3D.
 * ONLINE = estilo remoto de OpenFreeMap (gratis, sin límites de uso, sin API key) — se usa
 * mientras no haya datos locales pero sí haya internet. No incluye edificios en 3D.
 * NONE = sin mapa real disponible todavía (ni local ni internet); la pantalla llamante debe
 * mostrar el Canvas 2D de respaldo en vez de este composable.
 */
enum class CubaMapSource { LOCAL, ONLINE, NONE }

private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun CubaMapLibreView(
    userLocation: GpsLocationData,
    viewMode: CubaMapViewMode,
    mapSource: CubaMapSource,
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    MapLibre.getInstance(context)

val tileServer = remember { MbtilesTileServer(context) }
val mapView = remember { MapView(context) }
var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

DisposableEffect(lifecycleOwner) {
        if (tileServer.isMapDataAvailable()) {
            tileServer.openDatabase()
            runCatching { tileServer.start(NANO_TIMEOUT_MS, false) }
                .onFailure { Log.e("CubaMapLibreView", "No se pudo iniciar el servidor de tiles local", it) }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tileServer.stopServer()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            mapView.getMapAsync { map ->
                mapLibreMap = map
                onMapReady(map)
            }
            mapView
        }
    )

    // Aplica (o cambia sobre la marcha) el estilo según la fuente activa: local si ya se
    // descargó el mapa de Cuba, online (OpenFreeMap) si no, mientras haya internet.
    LaunchedEffect(mapLibreMap, mapSource) {
        val map = mapLibreMap ?: return@LaunchedEffect
        when (mapSource) {
            CubaMapSource.LOCAL -> map.setStyle(buildLocalStyleJsonUri(context, tileServer))
            CubaMapSource.ONLINE -> map.setStyle(OPENFREEMAP_STYLE_URL)
            CubaMapSource.NONE -> Unit
        }
    }

    LaunchedEffect(userLocation, viewMode) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val target = LatLng(userLocation.point.lat, userLocation.point.lon)
        val camera = when (viewMode) {
            CubaMapViewMode.NORMAL_2D -> CameraPosition.Builder()
                .target(target).zoom(15.0).tilt(0.0).bearing(0.0).build()
            CubaMapViewMode.FREE_3D -> CameraPosition.Builder()
                .target(target).zoom(16.5).tilt(55.0).bearing(userLocation.bearing.toDouble()).build()
            CubaMapViewMode.NAVIGATION_FPV -> CameraPosition.Builder()
                // Cámara "primera persona": muy inclinada, zoom cercano, orientada hacia
                // donde se dirige el usuario (bearing = rumbo GPS), como en Waze/Google Maps.
                .target(target).zoom(18.5).tilt(65.0).bearing(userLocation.bearing.toDouble()).build()
        }
        map.easeCamera(CameraUpdateFactory.newCameraPosition(camera), 600)
    }
}

private fun buildLocalStyleJsonUri(context: Context, tileServer: MbtilesTileServer): String {
    val templateText = context.assets.open("map_style/cuba_style.json").bufferedReader().use { it.readText() }
    val baseUrl = "http://127.0.0.1:8085"
    val resolved = templateText.replace("__TILE_SERVER_BASE_URL__", baseUrl)
    val outFile = File(context.cacheDir, "cuba_style_resolved.json")
    outFile.writeText(resolved)
    return "file://${outFile.absolutePath}"
}

private const val NANO_TIMEOUT_MS = 5000
