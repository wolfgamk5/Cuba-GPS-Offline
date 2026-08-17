package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CubaGeographyData
import com.example.data.GeoPoint
import com.example.data.PointOfInterest
import com.example.data.RouteResult
import com.example.data.RouteStep
import com.example.data.local.NavDatabase
import com.example.data.local.RouteHistoryEntity
import com.example.download.MapDataRepository
import com.example.download.MapDownloadStatus
import com.example.location.GpsLocationData
import com.example.location.LocationTracker
import com.example.maps.MbtilesTileServer
import com.example.network.NetworkMonitor
import com.example.routing.GraphHopperRoutingEngine
import com.example.routing.OfflineRoutingEngine
import com.example.routing.OnlineRoutingEngine
import com.example.tts.VoiceNavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CubaNavUiState(
    val origin: GeoPoint = CubaGeographyData.CITIES.first { it.name == "La Habana" },
    val destination: GeoPoint = CubaGeographyData.CITIES.first { it.name == "Varadero" },
    val activeRoute: RouteResult? = null,
    val isNavigating: Boolean = false,
    val isVoiceMuted: Boolean = false,
    val isNightMode: Boolean = true,
    val showPois: Boolean = true,
    val selectedPoi: PointOfInterest? = null,
    val currentStepIndex: Int = 0,
    val distanceToNextTurnMeters: Double = 0.0,
    val speedLimitKmh: Int = 100,
    val showPoiDialog: Boolean = false,
    val showOfflineMapsDialog: Boolean = false,
    val showStepsDialog: Boolean = false,
    val isRealRoutingReady: Boolean = false,
    val hasOfflineMapData: Boolean = false,
    val downloadStatus: MapDownloadStatus = MapDownloadStatus.NotStarted,
    val isOnline: Boolean = false
)

class CubaNavViewModel(application: Application) : AndroidViewModel(application) {

    // Motor de respaldo (6 carreteras troncales escritas a mano) — siempre disponible.
    private val fallbackRoutingEngine = OfflineRoutingEngine()
    // Motor real calle-por-calle sobre el grafo completo de OSM de Cuba, si ya se generó
    // y copió graph-cache/ al teléfono (ver SETUP_MAPA_OFFLINE.md, Fase 2).
    private val graphHopperEngine = GraphHopperRoutingEngine(application)
    private val onlineRoutingEngine = OnlineRoutingEngine()
    private val networkMonitor = NetworkMonitor(application)
    private val mapDataRepository = MapDataRepository(application)
    val locationTracker = LocationTracker(application)
    val voiceManager = VoiceNavigationManager(application)
    private val db = NavDatabase.getDatabase(application)

    private val _uiState = MutableStateFlow(
        CubaNavUiState(hasOfflineMapData = MbtilesTileServer(application).isMapDataAvailable())
    )
    val uiState: StateFlow<CubaNavUiState> = _uiState.asStateFlow()

    val locationFlow = locationTracker.locationFlow
    val compassFlow = locationTracker.compassHeading

    init {
        // Conectividad en vivo: decide si se puede complementar con mapa/rutas online.
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        // Progreso de la descarga automática del mapa/rutas (Fase 3). Cuando termina, se
        // recarga el motor de rutas y se activa el mapa 3D real sin reiniciar la app.
        viewModelScope.launch {
            mapDataRepository.observeDownloadStatus().collect { status ->
                _uiState.update { it.copy(downloadStatus = status) }
                if (status is MapDownloadStatus.Completed) {
                    _uiState.update { it.copy(hasOfflineMapData = mapDataRepository.isMbtilesReady()) }
                    val ready = withContext(Dispatchers.IO) { graphHopperEngine.load() }
                    _uiState.update { it.copy(isRealRoutingReady = ready) }
                    calculateRoute()
                }
            }
        }

        // Carga del grafo GraphHopper en segundo plano (operación de disco, nunca en el hilo
        // principal). Si no está disponible o falla, la app sigue funcionando con el motor
        // de respaldo sin que el usuario note ningún error.
        viewModelScope.launch {
            val ready = withContext(Dispatchers.IO) { graphHopperEngine.load() }
            _uiState.update { it.copy(isRealRoutingReady = ready) }
            if (ready) calculateRoute() // recalcula la ruta inicial ya con datos reales
        }

        // Automatically calculate initial preview route: La Habana -> Varadero (via Vía Blanca)
        calculateRoute()

        // Observe GPS updates to refresh step proximity and TTS announcements
        viewModelScope.launch {
            locationFlow.collect { loc ->
                updateNavigationProgress(loc.point)
            }
        }
    }

    fun setOrigin(point: GeoPoint) {
        _uiState.update { it.copy(origin = point) }
        calculateRoute()
    }

    fun setDestination(point: GeoPoint) {
        _uiState.update { it.copy(destination = point, selectedPoi = null) }
        calculateRoute()
    }

    fun setDestinationFromPoi(poi: PointOfInterest) {
        _uiState.update {
            it.copy(
                destination = poi.toGeoPoint(),
                selectedPoi = poi
            )
        }
        calculateRoute()
    }

    fun swapOriginAndDestination() {
        val currentOrigin = _uiState.value.origin
        val currentDest = _uiState.value.destination
        _uiState.update {
            it.copy(origin = currentDest, destination = currentOrigin)
        }
        calculateRoute()
    }

    fun calculateRoute() {
        val currentOrigin = _uiState.value.origin
        val currentDest = _uiState.value.destination

        viewModelScope.launch {
            // 1) Ruta real calle-por-calle si el grafo OSM local ya está cargado (rápido, sin red).
            // 2) Si no, ruta online (solo si el usuario configuró su propio servidor y hay internet).
            // 3) Si nada de eso está disponible, motor de respaldo con las 6 carreteras troncales.
            val result = withContext(Dispatchers.IO) {
                (if (graphHopperEngine.isLoaded) graphHopperEngine.calculateRoute(currentOrigin, currentDest) else null)
                    ?: (if (_uiState.value.isOnline) onlineRoutingEngine.calculateRoute(currentOrigin, currentDest) else null)
                    ?: fallbackRoutingEngine.calculateRoute(currentOrigin, currentDest)
            }

            _uiState.update {
                it.copy(
                    activeRoute = result,
                    currentStepIndex = 0,
                    distanceToNextTurnMeters = result.steps.firstOrNull()?.distanceMeters ?: 0.0,
                    speedLimitKmh = if (result.routeName.contains("Autopista", ignoreCase = true)) 100 else 80
                )
            }

            // Save to Room DB history
            db.navDao().insertRouteHistory(
                RouteHistoryEntity(
                    originName = currentOrigin.name,
                    originLat = currentOrigin.lat,
                    originLon = currentOrigin.lon,
                    destinationName = currentDest.name,
                    destinationLat = currentDest.lat,
                    destinationLon = currentDest.lon,
                    distanceKm = result.totalDistanceKm,
                    durationMinutes = result.estimatedTimeMinutes
                )
            )
        }
    }

    fun startRealNavigation() {
        val route = _uiState.value.activeRoute ?: return
        locationTracker.startRealTracking()
        _uiState.update {
            it.copy(isNavigating = true)
        }

        voiceManager.speak("Iniciando navegación hacia ${route.destination.name}. Distancia: ${route.totalDistanceKm} kilómetros.")
    }

    fun stopNavigation() {
        locationTracker.stopTracking()
        voiceManager.stop()
        _uiState.update {
            it.copy(
                isNavigating = false,
                currentStepIndex = 0
            )
        }
    }

    fun toggleVoice() {
        val newMuted = !_uiState.value.isVoiceMuted
        voiceManager.setMuted(newMuted)
        _uiState.update { it.copy(isVoiceMuted = newMuted) }
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun togglePois() {
        _uiState.update { it.copy(showPois = !it.showPois) }
    }

    fun openPoiDialog() = _uiState.update { it.copy(showPoiDialog = true) }
    fun closePoiDialog() = _uiState.update { it.copy(showPoiDialog = false) }

    fun openOfflineMapsDialog() = _uiState.update { it.copy(showOfflineMapsDialog = true) }
    fun closeOfflineMapsDialog() = _uiState.update { it.copy(showOfflineMapsDialog = false) }

    fun openStepsDialog() = _uiState.update { it.copy(showStepsDialog = true) }
    fun closeStepsDialog() = _uiState.update { it.copy(showStepsDialog = false) }

    /** Inicia la descarga en segundo plano del mapa 3D y las rutas reales de Cuba. Por
     *  defecto solo corre con WiFi; [allowMeteredData] la permite también con datos móviles. */
    fun startMapDataDownload(allowMeteredData: Boolean = false) {
        mapDataRepository.startDownload(allowMeteredData)
    }

    fun cancelMapDataDownload() {
        mapDataRepository.cancelDownload()
    }

    private fun updateNavigationProgress(currentLoc: GeoPoint) {
        val state = _uiState.value
        if (!state.isNavigating) return
        val route = state.activeRoute ?: return
        val steps = route.steps
        if (steps.isEmpty()) return

        var activeStepIdx = state.currentStepIndex
        val currentStep = steps.getOrNull(activeStepIdx) ?: return
        val distToTurn = currentLoc.distanceTo(currentStep.endPoint)

        // Advance to next step if passed the turn
        if (distToTurn < 30.0 && activeStepIdx < steps.size - 1) {
            activeStepIdx++
        }

        val stepToAnnounce = steps[activeStepIdx]
        val updatedDist = currentLoc.distanceTo(stepToAnnounce.endPoint)

        _uiState.update {
            it.copy(
                currentStepIndex = activeStepIdx,
                distanceToNextTurnMeters = updatedDist
            )
        }

        // Voice announcement
        voiceManager.announceStep(stepToAnnounce, updatedDist)
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.cleanup()
        voiceManager.shutdown()
        graphHopperEngine.close()
    }
}
