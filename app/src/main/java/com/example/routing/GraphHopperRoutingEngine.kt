package com.example.routing

import android.content.Context
import android.util.Log
import com.example.data.GeoPoint
import com.example.data.RouteResult
import com.example.data.RouteStep
import com.example.data.TurnType
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.config.Profile
import com.graphhopper.util.GHUtility
import com.graphhopper.util.Instruction
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Motor de rutas REAL, calle por calle, sobre el grafo completo de Cuba importado desde
 * OpenStreetMap con GraphHopper (a diferencia de [OfflineRoutingEngine], que solo conoce
 * un puñado de carreteras troncales escritas a mano).
 *
 * El grafo (`graph-cache/`) se genera UNA SOLA VEZ en una computadora a partir de
 * `cuba-latest.osm.pbf` — ver SETUP_MAPA_OFFLINE.md, sección "Fase 2" — y se copia al
 * teléfono junto al `cuba.mbtiles`. Aquí solo se abre en modo lectura; no se importa nada
 * en el propio dispositivo (sería demasiado lento/pesado para un teléfono).
 *
 * Nota: la API pública de GraphHopper (nombres de clases/métodos de configuración de
 * perfiles) cambia ligeramente entre versiones. Este código sigue el patrón estándar de la
 * versión declarada en libs.versions.toml; si Android Studio marca algún método como no
 * encontrado al abrir el proyecto, casi siempre es un ajuste menor de nombre de método para
 * la versión exacta que se haya resuelto — revisa la Javadoc de esa versión en
 * https://github.com/graphhopper/graphhopper/blob/master/docs/core/quickstart-from-source.md
 */
class GraphHopperRoutingEngine(context: Context) {

    private val graphCacheDir = File(context.getExternalFilesDir(null), "maps/graph-cache")
    private var hopper: GraphHopper? = null

    /** True si el usuario ya copió el grafo de rutas generado en su PC al teléfono. */
    val isAvailable: Boolean
        get() = graphCacheDir.exists() && graphCacheDir.isDirectory &&
            (graphCacheDir.listFiles()?.isNotEmpty() == true)

    /**
     * Abre el grafo. Es una operación de disco (varios cientos de ms a pocos segundos
     * dependiendo del tamaño del país) — SIEMPRE debe llamarse fuera del hilo principal.
     * Devuelve true si el motor quedó listo para calcular rutas.
     */
        fun load(): Boolean {
        if (!isAvailable) return false
        return try {
            val gh = GraphHopper()
            gh.graphHopperLocation = graphCacheDir.absolutePath
            gh.dataAccessType = "MMAP"
            gh.setEncodedValuesString("car_access, car_average_speed, road_access, road_environment, max_speed")
            gh.setProfiles(Profile("car").setCustomModel(GHUtility.loadCustomModelFromJar("car.json")))
            gh.importOrLoad()
            hopper = gh
            Log.i(TAG, "Grafo de rutas de Cuba cargado desde ${graphCacheDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar el grafo de rutas de Cuba", e)
            hopper = null
            false
        }
    }


    val isLoaded: Boolean get() = hopper != null

    /** Calcula una ruta real calle-por-calle. Devuelve null si el motor no está listo o si
     *  GraphHopper no encontró camino entre esos dos puntos (fuera de la red importada, etc). */
    fun calculateRoute(origin: GeoPoint, destination: GeoPoint): RouteResult? {
        val gh = hopper ?: return null
        val request = GHRequest(origin.lat, origin.lon, destination.lat, destination.lon)
            .setProfile("car")
            .setLocale(Locale("es"))

        val response = gh.route(request)
        if (response.hasErrors()) {
            Log.e(TAG, "Error calculando ruta con GraphHopper: ${response.errors}")
            return null
        }

        val path = response.best
        val translation = gh.translationMap.getWithFallBack(Locale("es"))

        val polyline = path.points.map { GeoPoint(it.lat, it.lon) }
        val steps = path.instructions.mapIndexed { index, instr ->
            val instrPoints = instr.points
            val start = instrPoints.firstOrNull()?.let { GeoPoint(it.lat, it.lon) } ?: origin
            val end = (if (instrPoints.size() > 1) instrPoints.lastOrNull() else instrPoints.firstOrNull())
                ?.let { GeoPoint(it.lat, it.lon) } ?: destination
            RouteStep(
                id = "gh_step_$index",
                instruction = instr.getTurnDescription(translation).ifBlank { instr.name },
                roadName = instr.name.ifBlank { "Vía sin nombre" },
                distanceMeters = instr.distance,
                turnType = mapSign(instr.sign),
                startPoint = start,
                endPoint = end
            )
        }
        val roadNames = path.instructions.mapNotNull { it.name.takeIf(String::isNotBlank) }.distinct()

        return RouteResult(
            origin = origin,
            destination = destination,
            totalDistanceKm = path.distance / 1000.0,
            estimatedTimeMinutes = (path.time / 60000.0).roundToInt(),
            polyline = polyline,
            steps = steps,
            routeName = "Ruta OSM · ${roadNames.take(3).joinToString(" → ").ifBlank { "Ruta calculada" }}",
            primaryRoads = roadNames
        )
    }

    private fun mapSign(sign: Int): TurnType = when (sign) {
        Instruction.U_TURN_UNKNOWN, Instruction.U_TURN_LEFT, Instruction.U_TURN_RIGHT -> TurnType.UTURN
        Instruction.KEEP_LEFT, Instruction.TURN_SLIGHT_LEFT -> TurnType.SLIGHT_LEFT
        Instruction.TURN_LEFT -> TurnType.LEFT
        Instruction.TURN_SHARP_LEFT -> TurnType.SHARP_LEFT
        Instruction.KEEP_RIGHT, Instruction.TURN_SLIGHT_RIGHT -> TurnType.SLIGHT_RIGHT
        Instruction.TURN_RIGHT -> TurnType.RIGHT
        Instruction.TURN_SHARP_RIGHT -> TurnType.SHARP_RIGHT
        Instruction.USE_ROUNDABOUT -> TurnType.ROUNDABOUT
        Instruction.REACHED_VIA, Instruction.FINISH -> TurnType.DESTINATION
        else -> TurnType.STRAIGHT
    }

    fun close() {
        hopper?.close()
        hopper = null
    }

    private companion object {
        const val TAG = "GraphHopperEngine"
    }
}
