package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.GeoPoint
import com.example.maps.CubaMapLibreView
import com.example.maps.CubaMapSource
import com.example.maps.CubaMapViewMode
import com.example.ui.CubaNavViewModel
import com.example.ui.components.CubaMapCanvas
import com.example.ui.components.NavigationSpeedometerCard
import com.example.ui.components.NavigationTopBanner
import com.example.ui.components.OfflineMapManagerDialog
import com.example.ui.components.PlayStoreGuideDialog
import com.example.ui.components.PoiExplorerDialog
import com.example.ui.components.RoutePlannerCard
import com.example.ui.components.RouteStepsDialog
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue

@Composable
fun CubaNavMainScreen(
    viewModel: CubaNavViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationData by viewModel.locationFlow.collectAsState()
    val compassHeading by viewModel.compassFlow.collectAsState()
    val context = LocalContext.current

    // ¿Ya existe el mapa real de Cuba (cuba.mbtiles, generado desde OSM)? Reactivo: se
    // actualiza solo cuando termina la descarga automática (Fase 3), sin reiniciar la app.
    val hasRealMapData = uiState.hasOfflineMapData
    // Fase 4: si no hay datos locales pero sí internet, se usa el mapa online (OpenFreeMap)
    // en vez del Canvas 2D de respaldo — la app funciona bien offline Y online.
    val mapSource = when {
        hasRealMapData -> CubaMapSource.LOCAL
        uiState.isOnline -> CubaMapSource.ONLINE
        else -> CubaMapSource.NONE
    }
    var mapViewMode by remember { mutableStateOf(CubaMapViewMode.FREE_3D) }

    // Durante la navegación activa, la cámara pasa automáticamente a primera persona.
    LaunchedEffect(uiState.isNavigating) {
        mapViewMode = if (uiState.isNavigating) CubaMapViewMode.NAVIGATION_FPV else CubaMapViewMode.FREE_3D
    }

    // Permission launcher for GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.locationTracker.startRealTracking()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavDeepBlue
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Mapa principal: real (MapLibre + datos OSM, con edificios 3D y cámara en
            // primera persona) si ya se generó cuba.mbtiles o si hay internet (OpenFreeMap);
            // si no hay ninguna de las dos, el Canvas 2D de respaldo dibujado a mano.
            if (mapSource != CubaMapSource.NONE) {
                CubaMapLibreView(
                    userLocation = locationData,
                    viewMode = mapViewMode,
                    mapSource = mapSource,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CubaMapCanvas(
                    userLocation = locationData,
                    activeRoute = uiState.activeRoute,
                    selectedPoi = uiState.selectedPoi,
                    isNightMode = uiState.isNightMode,
                    showPois = uiState.showPois,
                    onMapTap = { tappedPoint ->
                        viewModel.setDestination(tappedPoint)
                    },
                    onPoiTap = { poi ->
                        viewModel.setDestinationFromPoi(poi)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Top Bar Controls & Navigation Guidance Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // If navigating: Top Turn-by-Turn Guidance Banner
                AnimatedVisibility(
                    visible = uiState.isNavigating,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    val activeStep = uiState.activeRoute?.steps?.getOrNull(uiState.currentStepIndex)
                    NavigationTopBanner(
                        currentStep = activeStep,
                        distanceToNextTurnMeters = uiState.distanceToNextTurnMeters,
                        isVoiceMuted = uiState.isVoiceMuted,
                        onToggleVoice = { viewModel.toggleVoice() },
                        onStopNavigation = { viewModel.stopNavigation() }
                    )
                }

                // If not navigating: Minimal Clean Title & Status Bar
                if (!uiState.isNavigating) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.Start),
                        shape = RoundedCornerShape(16.dp),
                        color = NavDeepBlue.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (hasRealMapData) NavEmerald else NavAccentAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    hasRealMapData -> "Cuba GPS • 100% Offline"
                                    mapSource == CubaMapSource.ONLINE -> "Cuba GPS • En línea (mapa offline no descargado)"
                                    else -> "Cuba GPS • Sin mapa (necesitas internet o descargarlo)"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Floating Quick Action Tools (Right Side)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = if (uiState.isNavigating) 110.dp else 50.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // POI Explorer (CUPET / Hospitales)
                SmallFloatingActionButton(
                    onClick = { viewModel.openPoiDialog() },
                    containerColor = NavSurfaceBlue,
                    contentColor = NavCyan,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_open_pois")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Puntos de Interés Cuba"
                    )
                }

                // Alternar vista 2D clásica / 3D libre (solo con mapa real cargado; deshabilitado
                // durante la navegación, que siempre usa la cámara en primera persona)
                if (mapSource != CubaMapSource.NONE && !uiState.isNavigating) {
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewMode = if (mapViewMode == CubaMapViewMode.FREE_3D)
                                CubaMapViewMode.NORMAL_2D else CubaMapViewMode.FREE_3D
                        },
                        containerColor = NavSurfaceBlue,
                        contentColor = NavAccentAmber,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_toggle_3d")
                    ) {
                        Icon(
                            imageVector = if (mapViewMode == CubaMapViewMode.FREE_3D) Icons.Default.ViewInAr else Icons.Default.Layers,
                            contentDescription = "Alternar mapa 2D/3D"
                        )
                    }
                }

                // Offline Map Files Manager (.osm.pbf / .mbtiles)
                SmallFloatingActionButton(
                    onClick = { viewModel.openOfflineMapsDialog() },
                    containerColor = NavSurfaceBlue,
                    contentColor = NavAccentAmber,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_open_maps")
                ) {
                    Icon(
                        imageVector = Icons.Default.SdCard,
                        contentDescription = "Mapas Offline"
                    )
                }

                // Google Play Store Guide & Architecture Spec
                SmallFloatingActionButton(
                    onClick = { viewModel.openPlayStoreGuideDialog() },
                    containerColor = NavSurfaceBlue,
                    contentColor = NavEmerald,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_open_guide")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shop,
                        contentDescription = "Guía Play Store"
                    )
                }

                // Night Mode Toggle
                SmallFloatingActionButton(
                    onClick = { viewModel.toggleNightMode() },
                    containerColor = NavSurfaceBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_toggle_night")
                ) {
                    Icon(
                        imageVector = if (uiState.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Modo Noche/Día"
                    )
                }

                // Show/Hide POIs toggle
                SmallFloatingActionButton(
                    onClick = { viewModel.togglePois() },
                    containerColor = NavSurfaceBlue,
                    contentColor = if (uiState.showPois) NavCyan else Color.Gray,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_toggle_poi_visibility")
                ) {
                    Icon(
                        imageVector = if (uiState.showPois) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Alternar POIs"
                    )
                }

                // Step by Step Turn List
                if (uiState.activeRoute != null) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.openStepsDialog() },
                        containerColor = NavSurfaceBlue,
                        contentColor = NavCyan,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_open_steps")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Ver Pasos"
                        )
                    }
                }
            }

            // 4. Bottom Controls (Speedometer during navigation, or Route Planner otherwise)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp)
            ) {
                if (uiState.isNavigating) {
                    NavigationSpeedometerCard(
                        locationData = locationData,
                        activeRoute = uiState.activeRoute,
                        speedLimitKmh = uiState.speedLimitKmh
                    )
                } else {
                    RoutePlannerCard(
                        origin = uiState.origin,
                        destination = uiState.destination,
                        activeRoute = uiState.activeRoute,
                        onOriginSelected = { viewModel.setOrigin(it) },
                        onDestinationSelected = { viewModel.setDestination(it) },
                        onSwapPoints = { viewModel.swapOriginAndDestination() },
                        onCalculateRoute = { viewModel.calculateRoute() },
                        onStartNavigation = { viewModel.startRealNavigation() },
                        onStartSimulation = { viewModel.startSimulation() },
                        onViewSteps = { viewModel.openStepsDialog() }
                    )
                }
            }

            // 5. Dialog Overlays
            if (uiState.showPoiDialog) {
                PoiExplorerDialog(
                    hasRealPoiData = uiState.hasRealPoiData,
                    onSearchReal = { query, category -> viewModel.searchRealPois(query, category) },
                    onDismiss = { viewModel.closePoiDialog() },
                    onSelectPoiAsDestination = { poi ->
                        viewModel.setDestinationFromPoi(poi)
                    }
                )
            }

            if (uiState.showOfflineMapsDialog) {
                OfflineMapManagerDialog(
                    hasOfflineMapData = uiState.hasOfflineMapData,
                    isRealRoutingReady = uiState.isRealRoutingReady,
                    hasRealPoiData = uiState.hasRealPoiData,
                    downloadStatus = uiState.downloadStatus,
                    onStartDownload = { allowMetered -> viewModel.startMapDataDownload(allowMetered) },
                    onCancelDownload = { viewModel.cancelMapDataDownload() },
                    onDismiss = { viewModel.closeOfflineMapsDialog() }
                )
            }

            if (uiState.showPlayStoreGuideDialog) {
                PlayStoreGuideDialog(
                    onDismiss = { viewModel.closePlayStoreGuideDialog() }
                )
            }

            if (uiState.showStepsDialog && uiState.activeRoute != null) {
                RouteStepsDialog(
                    route = uiState.activeRoute!!,
                    onDismiss = { viewModel.closeStepsDialog() }
                )
            }
        }
    }
}
