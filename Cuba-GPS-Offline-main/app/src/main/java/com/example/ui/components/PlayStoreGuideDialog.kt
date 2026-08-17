package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue

@Composable
fun PlayStoreGuideDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(640.dp)
                .testTag("play_store_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavDeepBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shop,
                            contentDescription = null,
                            tint = NavCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guía Técnica Google Play & Stack",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Stack Offline Recomendado
                    item {
                        GuideSectionCard(
                            icon = Icons.Default.Map,
                            title = "1. Stack Tecnológico Offline (Open Source)",
                            accentColor = NavCyan
                        ) {
                            Text(
                                text = "• Renderizado de Mapa Offline:\n  - MapLibre Native SDK (OpenGL/Vulkan alta tasa de FPS con archivos .mbtiles o .pbf).\n  - OSMDroid (Alternativa ligera para teselas raster offline en SQLite/ZIP).\n  - Mapsforge (Ideal para renderizado vectorial puro desde archivos .map comprimidos).\n\n• Motor de Rutas Embebido:\n  - GraphHopper Core (Java/Kotlin embebido, enrutamiento instantáneo en milisegundos sin internet).\n  - Algoritmo A* / Contraction Hierarchies.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f)),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Section 2: Requisitos Obligatorios de Google Play Store
                    item {
                        GuideSectionCard(
                            icon = Icons.Default.Security,
                            title = "2. Requisitos Técnicos Google Play (2024 - 2026)",
                            accentColor = NavEmerald
                        ) {
                            Text(
                                text = "✔ Formato AAB (Android App Bundle):\n  Google Play exige formato .aab (no APK directo) para distribución.\n\n✔ Target SDK:\n  targetSdkVersion 34 o superior (Android 14+ / 15).\n\n✔ Arquitectura 64-Bit:\n  Incluir librerías nativas para arm64-v8a y x86_64.\n\n✔ Permisos de Ubicación & Foreground Service:\n  Declarar FOREGROUND_SERVICE_LOCATION y justificar la navegación paso a paso ante el equipo de revisión de Google Play.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f)),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Section 3: Generación de Keystore y Firma
                    item {
                        GuideSectionCard(
                            icon = Icons.Default.Key,
                            title = "3. Generar Keystore y Firma en Android Studio",
                            accentColor = NavAccentAmber
                        ) {
                            Text(
                                text = "Comando de consola para generar tu clave de firma:\nkeytool -genkey -v -keystore mi-cuba-gps-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias cubagps\n\nEn Android Studio:\nBuild > Generate Signed Bundle / APK > Android App Bundle > Seleccionar Keystore > release.",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NavAccentAmber,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Section 4: Manejo de Archivos .PBF de Cuba
                    item {
                        GuideSectionCard(
                            icon = Icons.Default.Code,
                            title = "4. Cargar Mapa de Cuba (.pbf / .mbtiles)",
                            accentColor = NavCyan
                        ) {
                            Text(
                                text = "1. Descargar el extracto oficial de Cuba desde Geofabrik:\n   https://download.geofabrik.de/central-america/cuba-latest.osm.pbf (~95 MB).\n\n2. Colocarlo en el almacenamiento interno de la app con Context.getExternalFilesDir(\"maps\").\n\n3. GraphHopper o MapLibre lo indexa automáticamente en el primer inicio de la app.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f)),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NavSurfaceBlue
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
