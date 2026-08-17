package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.CubaGeographyData
import com.example.data.GeoPoint
import com.example.data.PoiCategory
import com.example.data.PointOfInterest
import com.example.data.RouteResult
import com.example.location.GpsLocationData
import com.example.ui.theme.MapLandDay
import com.example.ui.theme.MapLandNight
import com.example.ui.theme.MapRoadHighwayDay
import com.example.ui.theme.MapRoadHighwayNight
import com.example.ui.theme.MapRoadPrimaryDay
import com.example.ui.theme.MapRoadPrimaryNight
import com.example.ui.theme.MapWaterDay
import com.example.ui.theme.MapWaterNight
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCoralRed
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavRouteBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CubaMapCanvas(
    userLocation: GpsLocationData,
    activeRoute: RouteResult?,
    selectedPoi: PointOfInterest?,
    isNightMode: Boolean,
    showPois: Boolean,
    onMapTap: (GeoPoint) -> Unit,
    onPoiTap: (PointOfInterest) -> Unit,
    modifier: Modifier = Modifier
) {
    // Map Viewport state (Center Lat/Lon, Zoom scale, Pan Offset)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Colors according to Day / Night navigation mode
    val waterColor = if (isNightMode) MapWaterNight else MapWaterDay
    val landColor = if (isNightMode) MapLandNight else MapLandDay
    val primaryRoadColor = if (isNightMode) MapRoadPrimaryNight else MapRoadPrimaryDay
    val highwayColor = if (isNightMode) MapRoadHighwayNight else MapRoadHighwayDay

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(waterColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.6f, 15.0f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        // Convert screen pixel tap back to GeoPoint
                        // Handled dynamically based on current viewport
                    },
                    onDoubleTap = {
                        zoomScale = (zoomScale * 1.5f).coerceAtMost(15.0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Mapping projection: Cuba Longitude (-85.0 to -74.0) -> X, Latitude (19.8 to 23.3) -> Y
            val minLon = CubaGeographyData.CUBA_BOUNDS_MIN_LON
            val maxLon = CubaGeographyData.CUBA_BOUNDS_MAX_LON
            val minLat = CubaGeographyData.CUBA_BOUNDS_MIN_LAT
            val maxLat = CubaGeographyData.CUBA_BOUNDS_MAX_LAT

            fun projectGeo(point: GeoPoint): Offset {
                val normX = (point.lon - minLon) / (maxLon - minLon)
                // Invert Y because Latitude goes up (North) while Canvas Y goes down
                val normY = 1.0 - ((point.lat - minLat) / (maxLat - minLat))

                val basePixelX = (normX * canvasWidth).toFloat()
                val basePixelY = (normY * canvasHeight * 0.75f + canvasHeight * 0.12f).toFloat()

                // Apply zoom around center and pan
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                val finalX = (basePixelX - centerX) * zoomScale + centerX + panOffsetX
                val finalY = (basePixelY - centerY) * zoomScale + centerY + panOffsetY

                return Offset(finalX, finalY)
            }

            // 1. Draw stylized Cuban Archipelago landmass
            drawCubanLandmass(landColor, ::projectGeo)

            // 2. Draw Provincial Borders / Shading
            drawProvinceBorders(if (isNightMode) Color(0x33FFFFFF) else Color(0x33000000), ::projectGeo)

            // 3. Draw Road Network (Autopista Nacional, Carretera Central, Vía Blanca)
            drawRoadNetwork(primaryRoadColor, highwayColor, ::projectGeo, zoomScale)

            // 4. Draw Calculated Route Polyline if present
            activeRoute?.let { route ->
                drawActiveRoute(route, ::projectGeo)
            }

            // 5. Draw Major Cities & Labels
            drawCities(isNightMode, ::projectGeo, zoomScale)

            // 6. Draw Points of Interest (Gasolineras CUPET, Hospitales, Hoteles, Talleres)
            if (showPois) {
                drawPointsOfInterest(CubaGeographyData.POI_DATABASE, selectedPoi, isNightMode, ::projectGeo, zoomScale)
            }

            // 7. Draw User Vehicle / GPS Marker with Heading Cone
            drawUserVehicleMarker(userLocation, isNightMode, ::projectGeo)
        }
    }
}

/**
 * Draws the polygonal shape of the island of Cuba and Isla de la Juventud
 */
private fun DrawScope.drawCubanLandmass(
    landColor: Color,
    project: (GeoPoint) -> Offset
) {
    // Key coastline boundary points defining Cuba's shape
    val cubaCoastline = listOf(
        GeoPoint(21.85, -84.95), // Cabo San Antonio (Extremo Occidental)
        GeoPoint(22.10, -84.50),
        GeoPoint(22.80, -83.80),
        GeoPoint(23.00, -83.20),
        GeoPoint(23.15, -82.40), // La Habana
        GeoPoint(23.18, -81.50), // Matanzas
        GeoPoint(23.22, -81.15), // Varadero / Cárdenas
        GeoPoint(23.00, -80.50),
        GeoPoint(22.65, -79.50), // Bahía de Buena Vista / Caibarién
        GeoPoint(22.40, -78.60), // Cayo Coco
        GeoPoint(21.75, -77.50), // Playa Santa Lucía
        GeoPoint(21.40, -76.80), // Puerto Padre
        GeoPoint(21.15, -75.70), // Guardalavaca / Gibara
        GeoPoint(20.70, -75.00), // Moa
        GeoPoint(20.40, -74.15), // Punta de Maisí (Extremo Oriental)
        GeoPoint(20.00, -74.30),
        GeoPoint(19.90, -75.10), // Guantánamo
        GeoPoint(19.95, -75.85), // Santiago de Cuba
        GeoPoint(19.85, -76.80), // Pico Turquino / Sierra Maestra
        GeoPoint(19.82, -77.70), // Cabo Cruz
        GeoPoint(20.40, -77.20), // Golfo de Guacanayabo
        GeoPoint(20.70, -77.80),
        GeoPoint(21.50, -79.00), // Júcaro
        GeoPoint(21.60, -79.90), // Casilda / Trinidad
        GeoPoint(22.10, -80.50), // Cienfuegos
        GeoPoint(22.05, -81.50), // Bahía de Cochinos / Península de Zapata
        GeoPoint(22.30, -82.60), // Golfo de Batabanó
        GeoPoint(22.00, -83.50),
        GeoPoint(21.85, -84.30),
        GeoPoint(21.85, -84.95) // Close loop
    )

    val path = Path()
    val startOffset = project(cubaCoastline[0])
    path.moveTo(startOffset.x, startOffset.y)

    for (i in 1 until cubaCoastline.size) {
        val pt = project(cubaCoastline[i])
        path.lineTo(pt.x, pt.y)
    }
    path.close()

    drawPath(
        path = path,
        color = landColor
    )

    // Isla de la Juventud
    val islaJuventud = listOf(
        GeoPoint(21.95, -82.80),
        GeoPoint(21.85, -82.60),
        GeoPoint(21.55, -82.70),
        GeoPoint(21.50, -83.00),
        GeoPoint(21.75, -83.15),
        GeoPoint(21.95, -82.80)
    )
    val islaPath = Path()
    val islaStart = project(islaJuventud[0])
    islaPath.moveTo(islaStart.x, islaStart.y)
    for (i in 1 until islaJuventud.size) {
        val pt = project(islaJuventud[i])
        islaPath.lineTo(pt.x, pt.y)
    }
    islaPath.close()

    drawPath(path = islaPath, color = landColor)
}

private fun DrawScope.drawProvinceBorders(
    borderColor: Color,
    project: (GeoPoint) -> Offset
) {
    // Subtle provincial division lines across the island
    val provinceDivisions = listOf(
        listOf(GeoPoint(22.40, -83.50), GeoPoint(22.90, -83.50)), // Pinar / Artemisa
        listOf(GeoPoint(22.75, -82.40), GeoPoint(23.10, -82.40)), // Artemisa / Habana
        listOf(GeoPoint(22.70, -81.70), GeoPoint(23.15, -81.70)), // Mayabeque / Matanzas
        listOf(GeoPoint(22.10, -80.70), GeoPoint(22.80, -80.70)), // Matanzas / Cienfuegos & VC
        listOf(GeoPoint(21.80, -79.80), GeoPoint(22.50, -79.80)), // Cienfuegos / Sancti Spíritus
        listOf(GeoPoint(21.60, -78.90), GeoPoint(22.30, -78.90)), // Sancti Spíritus / Ciego
        listOf(GeoPoint(21.20, -78.10), GeoPoint(21.90, -78.10)), // Ciego / Camagüey
        listOf(GeoPoint(20.80, -77.10), GeoPoint(21.40, -77.10)), // Camagüey / Las Tunas
        listOf(GeoPoint(20.60, -76.40), GeoPoint(21.10, -76.40)), // Las Tunas / Holguín
        listOf(GeoPoint(20.10, -76.00), GeoPoint(20.80, -76.00)), // Granma / Santiago
        listOf(GeoPoint(19.90, -75.30), GeoPoint(20.50, -75.30))  // Santiago / Guantánamo
    )

    for (div in provinceDivisions) {
        val p1 = project(div[0])
        val p2 = project(div[1])
        drawLine(
            color = borderColor,
            start = p1,
            end = p2,
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }
}

private fun DrawScope.drawRoadNetwork(
    primaryColor: Color,
    highwayColor: Color,
    project: (GeoPoint) -> Offset,
    zoom: Float
) {
    for (segment in CubaGeographyData.TRUNK_ROADS) {
        val isHighway = segment.highwayType == "motorway"
        val strokeW = if (isHighway) (4.5f * zoom.coerceIn(1f, 3.5f)) else (3f * zoom.coerceIn(1f, 3f))
        val color = if (isHighway) highwayColor else primaryColor

        for (i in 0 until segment.points.size - 1) {
            val p1 = project(segment.points[i])
            val p2 = project(segment.points[i + 1])

            // Draw road casing (dark border for clarity)
            drawLine(
                color = Color(0x66000000),
                start = p1,
                end = p2,
                strokeWidth = strokeW + 2f,
                cap = StrokeCap.Round
            )
            // Draw road center
            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawActiveRoute(
    route: RouteResult,
    project: (GeoPoint) -> Offset
) {
    val polyline = route.polyline
    if (polyline.size < 2) return

    val path = Path()
    val start = project(polyline[0])
    path.moveTo(start.x, start.y)

    for (i in 1 until polyline.size) {
        val pt = project(polyline[i])
        path.lineTo(pt.x, pt.y)
    }

    // Route Outer Glow / Casing
    drawPath(
        path = path,
        color = Color(0x8800B4D8),
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Route Main Line (Vibrant Blue)
    drawPath(
        path = path,
        color = NavRouteBlue,
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Route Center Accent
    drawPath(
        path = path,
        color = Color.White,
        style = Stroke(
            width = 3f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
        )
    )

    // Start Pin (Green)
    val startPos = project(polyline.first())
    drawCircle(color = Color.White, radius = 9f, center = startPos)
    drawCircle(color = NavEmerald, radius = 7f, center = startPos)

    // Destination Pin (Red Flag)
    val destPos = project(polyline.last())
    drawCircle(color = Color.White, radius = 11f, center = destPos)
    drawCircle(color = NavCoralRed, radius = 9f, center = destPos)
    drawCircle(color = Color.White, radius = 4f, center = destPos)
}

private fun DrawScope.drawCities(
    isNightMode: Boolean,
    project: (GeoPoint) -> Offset,
    zoom: Float
) {
    val textColor = if (isNightMode) Color.White else Color(0xFF1E293B)

    for (city in CubaGeographyData.CITIES) {
        val pos = project(city)
        val isCapital = city.name == "La Habana"

        val dotRadius = if (isCapital) 6f else 4f
        val dotColor = if (isCapital) NavAccentAmber else Color.White

        // City dot
        drawCircle(color = Color(0x99000000), radius = dotRadius + 2f, center = pos)
        drawCircle(color = dotColor, radius = dotRadius, center = pos)
    }
}

private fun DrawScope.drawPointsOfInterest(
    pois: List<PointOfInterest>,
    selectedPoi: PointOfInterest?,
    isNightMode: Boolean,
    project: (GeoPoint) -> Offset,
    zoom: Float
) {
    for (poi in pois) {
        val pos = project(poi.toGeoPoint())
        val isSelected = selectedPoi?.id == poi.id

        val pinColor = when (poi.category) {
            PoiCategory.GASOLINERA -> NavAccentAmber
            PoiCategory.HOSPITAL -> NavCoralRed
            PoiCategory.HOTEL_CAMPISMO -> NavCyan
            PoiCategory.TALLER_PONCHERA -> Color(0xFFFF8C00)
            PoiCategory.FARMACIA -> NavEmerald
            PoiCategory.TRANSPORTE -> Color(0xFF9333EA)
            PoiCategory.TURISMO -> Color(0xFF06B6D4)
            PoiCategory.BANCO_CAJERO -> Color(0xFF10B981)
            PoiCategory.GASTRONOMIA -> Color(0xFFF97316)
        }

        val baseRadius = if (isSelected) 10f else 6.5f

        // Pin shadow
        drawCircle(
            color = Color(0x66000000),
            radius = baseRadius + 3f,
            center = Offset(pos.x + 1f, pos.y + 2f)
        )
        // Pin border
        drawCircle(
            color = Color.White,
            radius = baseRadius + 1.5f,
            center = pos
        )
        // Pin center
        drawCircle(
            color = pinColor,
            radius = baseRadius,
            center = pos
        )

        if (isSelected) {
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = pos
            )
        }
    }
}

private fun DrawScope.drawUserVehicleMarker(
    userLoc: GpsLocationData,
    isNightMode: Boolean,
    project: (GeoPoint) -> Offset
) {
    val pos = project(userLoc.point)
    val bearing = userLoc.bearing

    rotate(degrees = bearing, pivot = pos) {
        // 1. Heading light beam cone (vision cone)
        val beamPath = Path()
        beamPath.moveTo(pos.x, pos.y)
        beamPath.lineTo(pos.x - 35f, pos.y - 75f)
        beamPath.lineTo(pos.x + 35f, pos.y - 75f)
        beamPath.close()

        drawPath(
            path = beamPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0x6600B4D8), Color(0x0000B4D8)),
                center = pos,
                radius = 80f
            )
        )

        // 2. Outer accuracy ripple
        drawCircle(
            color = Color(0x3300B4D8),
            radius = 24f,
            center = pos
        )

        // 3. Navigation Arrow Vehicle Icon (Clean GPS Puck)
        val arrowPath = Path()
        arrowPath.moveTo(pos.x, pos.y - 18f) // Tip pointing Forward
        arrowPath.lineTo(pos.x + 12f, pos.y + 12f)
        arrowPath.lineTo(pos.x, pos.y + 5f)
        arrowPath.lineTo(pos.x - 12f, pos.y + 12f)
        arrowPath.close()

        // Shadow
        drawPath(
            path = arrowPath,
            color = Color(0x55000000),
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Fill
        drawPath(
            path = arrowPath,
            color = NavCyan
        )
        drawPath(
            path = arrowPath,
            color = Color.White,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Vehicle Center Dot
    drawCircle(color = Color.White, radius = 3.5f, center = pos)
}
