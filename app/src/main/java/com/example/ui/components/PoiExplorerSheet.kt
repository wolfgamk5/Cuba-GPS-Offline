package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CubaGeographyData
import com.example.data.PoiCategory
import com.example.data.PointOfInterest
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue

@Composable
fun PoiExplorerDialog(
    onDismiss: () -> Unit,
    onSelectPoiAsDestination: (PointOfInterest) -> Unit,
    hasRealPoiData: Boolean = false,
    onSearchReal: suspend (query: String, category: PoiCategory?) -> List<PointOfInterest>? = { _, _ -> null },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PoiCategory?>(null) }
    var realResults by remember { mutableStateOf<List<PointOfInterest>?>(null) }

    // Con datos reales de OSM: buscamos en la base descargada (miles de lugares en todo el
    // país). Sin ellos: filtramos la lista curada de 38 lugares de respaldo, igual que antes.
    LaunchedEffect(searchQuery, selectedCategory, hasRealPoiData) {
        realResults = if (hasRealPoiData) onSearchReal(searchQuery, selectedCategory) else null
    }

    val filteredPois = realResults ?: remember(searchQuery, selectedCategory) {
        CubaGeographyData.POI_DATABASE.filter { poi ->
            val matchesCategory = selectedCategory == null || poi.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    poi.name.contains(searchQuery, ignoreCase = true) ||
                    poi.province.contains(searchQuery, ignoreCase = true) ||
                    poi.address.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(600.dp)
                .testTag("poi_explorer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavDeepBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Puntos de Interés en Cuba",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavCyan
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar CUPET, Hospital, Hotel, Taller...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poi_search_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(if (hasRealPoiData) "Todos" else "Todos (${CubaGeographyData.POI_DATABASE.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavCyan,
                                selectedLabelColor = NavDeepBlue
                            )
                        )
                    }
                    items(PoiCategory.entries.toTypedArray()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.displayName.split("/")[0].trim()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getPoiIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavCyan,
                                selectedLabelColor = NavDeepBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // POI List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPois) { poi ->
                        PoiListItem(
                            poi = poi,
                            onRouteTo = {
                                onSelectPoiAsDestination(poi)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoiListItem(
    poi: PointOfInterest,
    onRouteTo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NavSurfaceBlue
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(NavCyan.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPoiIcon(poi.category),
                    contentDescription = null,
                    tint = NavCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${poi.province} • ${poi.address}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    ),
                    maxLines = 1
                )
                if (poi.phone.isNotBlank()) {
                    Text(
                        text = "Tel: ${poi.phone}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NavAccentAmber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRouteTo,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavCyan,
                    contentColor = NavDeepBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ruta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getPoiIcon(category: PoiCategory): ImageVector {
    return when (category) {
        PoiCategory.GASOLINERA -> Icons.Default.LocalGasStation
        PoiCategory.HOSPITAL -> Icons.Default.LocalHospital
        PoiCategory.HOTEL_CAMPISMO -> Icons.Default.Hotel
        PoiCategory.TALLER_PONCHERA -> Icons.Default.Build
        PoiCategory.FARMACIA -> Icons.Default.LocalPharmacy
        PoiCategory.TRANSPORTE -> Icons.Default.DirectionsBus
        PoiCategory.TURISMO -> Icons.Default.Attractions
        PoiCategory.BANCO_CAJERO -> Icons.Default.AccountBalance
        PoiCategory.GASTRONOMIA -> Icons.Default.Restaurant
    }
}
