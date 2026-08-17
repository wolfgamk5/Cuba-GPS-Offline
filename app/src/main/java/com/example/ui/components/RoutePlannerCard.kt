package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CubaGeographyData
import com.example.data.GeoPoint
import com.example.data.RouteResult
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerCard(
    origin: GeoPoint,
    destination: GeoPoint,
    activeRoute: RouteResult?,
    onOriginSelected: (GeoPoint) -> Unit,
    onDestinationSelected: (GeoPoint) -> Unit,
    onSwapPoints: () -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onViewSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedOrigin by remember { mutableStateOf(false) }
    var expandedDest by remember { mutableStateOf(false) }
    var showStepList by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("route_planner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavDeepBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Planificador de Rutas Offline",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavCyan
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Origin Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Origen",
                    tint = NavEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedOrigin,
                    onExpandedChange = { expandedOrigin = !expandedOrigin },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = origin.name.ifEmpty { "Origen: (${origin.lat}, ${origin.lon})" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigin) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("origin_selector_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedOrigin,
                        onDismissRequest = { expandedOrigin = false },
                        modifier = Modifier.background(NavSurfaceBlue)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mi Ubicación GPS Actual", color = NavEmerald, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onOriginSelected(CubaGeographyData.HAVANA_CENTER.copy(name = "Mi Ubicación GPS"))
                                expandedOrigin = false
                            }
                        )
                        CubaGeographyData.CITIES.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.name, color = Color.White) },
                                onClick = {
                                    onOriginSelected(city)
                                    expandedOrigin = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSwapPoints,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(38.dp)
                        .testTag("swap_points_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Intercambiar",
                        tint = NavCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Destination Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Destino",
                    tint = NavAccentAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDest,
                    onExpandedChange = { expandedDest = !expandedDest },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = destination.name.ifEmpty { "Destino: (${destination.lat}, ${destination.lon})" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDest) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dest_selector_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDest,
                        onDismissRequest = { expandedDest = false },
                        modifier = Modifier.background(NavSurfaceBlue)
                    ) {
                        CubaGeographyData.CITIES.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.name, color = Color.White) },
                                onClick = {
                                    onDestinationSelected(city)
                                    expandedDest = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Route Calculation Summary if available
            if (activeRoute != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NavSurfaceBlue
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = NavCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeRoute.totalDistanceKm} km",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                            }
                            Text(
                                text = "${activeRoute.estimatedTimeMinutes / 60} h ${activeRoute.estimatedTimeMinutes % 60} min",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavAccentAmber
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeRoute.routeName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeRoute == null) {
                    Button(
                        onClick = onCalculateRoute,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("calculate_route_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavCyan,
                            contentColor = NavDeepBlue
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Route, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calcular Ruta", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStartNavigation,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_nav_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Navegar GPS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
