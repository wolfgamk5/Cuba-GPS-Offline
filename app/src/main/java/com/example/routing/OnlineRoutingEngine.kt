package com.example.routing

import android.util.Log
import com.example.BuildConfig
import com.example.data.GeoPoint
import com.example.data.RouteResult
import com.example.data.RouteStep
import com.example.data.TurnType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Motor de rutas ONLINE opcional (Fase 4): consulta un servidor propio compatible con la API
 * HTTP de OSRM (https://project-osrm.org) para calcular rutas cuando no hay datos offline
 * descargados todavía, pero sí hay internet.
 *
 * IMPORTANTE — por qué esto está desactivado por defecto: el servidor de demostración público
 * de OSRM (router.project-osrm.org) prohíbe explícitamente el uso "pesado" o de producción en
 * su política de uso (máx. ~1 solicitud/segundo, "no es una API lista para producción"). Usarlo
 * como backend por defecto de una app real terminaría bloqueado y podría afectar a otros
 * proyectos que sí lo usan de forma legítima para pruebas puntuales. Por eso este motor solo
 * se activa si TÚ configuras la URL de tu propio servidor (autohospedado con Docker, es
 * gratis y usa el mismo `cuba-latest.osm.pbf` — ver SETUP_MAPA_OFFLINE.md, Fase 4) o de un
 * proveedor comercial que hable el mismo protocolo. Si [BuildConfig.ONLINE_ROUTING_BASE_URL]
 * está vacío, esta clase simplemente no se usa y la app sigue con GraphHopper local o el
 * motor de respaldo.
 */
class OnlineRoutingEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(OsrmRouteResponse::class.java)

    val isConfigured: Boolean get() = BuildConfig.ONLINE_ROUTING_BASE_URL.isNotBlank()

    suspend fun calculateRoute(origin: GeoPoint, destination: GeoPoint): RouteResult? {
        if (!isConfigured) return null
        val baseUrl = BuildConfig.ONLINE_ROUTING_BASE_URL.trimEnd('/')
        val url = "$baseUrl/route/v1/driving/${origin.lon},${origin.lat};${destination.lon},${destination.lat}" +
            "?overview=full&geometries=geojson&steps=true&alternatives=false"

        return try {
            val request = Request.Builder().url(url).header("User-Agent", "CubaGpsOffline/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val parsed = adapter.fromJson(body) ?: return null
                toRouteResult(parsed, origin, destination)
            }
        } catch (e: Exception) {
            Log.e("OnlineRoutingEngine", "No se pudo calcular ruta online", e)
            null
        }
    }

    private fun toRouteResult(response: OsrmRouteResponse, origin: GeoPoint, destination: GeoPoint): RouteResult? {
        val route = response.routes?.firstOrNull() ?: return null
        val coords = route.geometry?.coordinates.orEmpty()
        val polyline = coords.map { GeoPoint(lat = it.getOrElse(1) { destination.lat }, lon = it.getOrElse(0) { destination.lon }) }

        val steps = route.legs.orEmpty().flatMap { it.steps.orEmpty() }.mapIndexed { index, step ->
            val loc = step.maneuver?.location
            val point = loc?.let { GeoPoint(it.getOrElse(1) { origin.lat }, it.getOrElse(0) { origin.lon }) } ?: origin
            RouteStep(
                id = "osrm_step_$index",
                instruction = describeManeuver(step.maneuver?.type, step.maneuver?.modifier, step.name),
                roadName = step.name?.ifBlank { "Vía sin nombre" } ?: "Vía sin nombre",
                distanceMeters = step.distance ?: 0.0,
                turnType = mapManeuver(step.maneuver?.type, step.maneuver?.modifier),
                startPoint = point,
                endPoint = point
            )
        }

        val roadNames = steps.map { it.roadName }.distinct()

        return RouteResult(
            origin = origin,
            destination = destination,
            totalDistanceKm = (route.distance ?: 0.0) / 1000.0,
            estimatedTimeMinutes = ((route.duration ?: 0.0) / 60.0).roundToInt(),
            polyline = polyline.ifEmpty { listOf(origin, destination) },
            steps = steps,
            routeName = "Ruta en línea · ${roadNames.take(3).joinToString(" → ").ifBlank { "Ruta calculada" }}",
            primaryRoads = roadNames
        )
    }

    private fun mapManeuver(type: String?, modifier: String?): TurnType = when {
        type == "arrive" -> TurnType.DESTINATION
        type == "depart" -> TurnType.START
        type == "roundabout" || type == "rotary" -> TurnType.ROUNDABOUT
        modifier == "sharp left" -> TurnType.SHARP_LEFT
        modifier == "left" -> TurnType.LEFT
        modifier == "slight left" -> TurnType.SLIGHT_LEFT
        modifier == "sharp right" -> TurnType.SHARP_RIGHT
        modifier == "right" -> TurnType.RIGHT
        modifier == "slight right" -> TurnType.SLIGHT_RIGHT
        modifier == "uturn" -> TurnType.UTURN
        else -> TurnType.STRAIGHT
    }

    private fun describeManeuver(type: String?, modifier: String?, roadName: String?): String {
        val road = roadName?.takeIf(String::isNotBlank)?.let { " hacia $it" } ?: ""
        return when {
            type == "arrive" -> "Ha llegado a su destino"
            type == "depart" -> "Comience la ruta$road"
            type == "roundabout" || type == "rotary" -> "Tome la rotonda$road"
            modifier == "left" -> "Gire a la izquierda$road"
            modifier == "sharp left" -> "Gire fuerte a la izquierda$road"
            modifier == "slight left" -> "Manténgase a la izquierda$road"
            modifier == "right" -> "Gire a la derecha$road"
            modifier == "sharp right" -> "Gire fuerte a la derecha$road"
            modifier == "slight right" -> "Manténgase a la derecha$road"
            modifier == "uturn" -> "Haga un cambio de sentido"
            else -> "Continúe recto$road"
        }
    }
}

// --- Modelos mínimos para parsear la respuesta JSON estándar de OSRM ---
private data class OsrmRouteResponse(val routes: List<OsrmRoute>?)
private data class OsrmRoute(
    val distance: Double?,
    val duration: Double?,
    val geometry: OsrmGeometry?,
    val legs: List<OsrmLeg>?
)
private data class OsrmGeometry(val coordinates: List<List<Double>>?)
private data class OsrmLeg(val steps: List<OsrmStep>?)
private data class OsrmStep(
    val distance: Double?,
    val duration: Double?,
    val name: String?,
    val maneuver: OsrmManeuver?
)
private data class OsrmManeuver(val type: String?, val modifier: String?, val location: List<Double>?)
