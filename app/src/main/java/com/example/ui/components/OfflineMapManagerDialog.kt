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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.download.MapDataDownloadWorker
import com.example.download.MapDownloadStatus
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCoralRed
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue

@Composable
fun OfflineMapManagerDialog(
    hasOfflineMapData: Boolean,
    isRealRoutingReady: Boolean,
    hasRealPoiData: Boolean,
    downloadStatus: MapDownloadStatus,
    onStartDownload: (allowMeteredData: Boolean) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var allowMeteredData by remember { mutableStateOf(false) }
    val isAllReady = hasOfflineMapData && isRealRoutingReady && hasRealPoiData
    val isDownloading = downloadStatus is MapDownloadStatus.Downloading

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("offline_map_manager_dialog"),
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
                            imageVector = Icons.Default.SdCard,
                            contentDescription = null,
                            tint = NavCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mapa y Rutas de Cuba",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barra de progreso global si está descargando
                if (isDownloading) {
                    OverallDownloadProgress(status = downloadStatus as MapDownloadStatus.Downloading)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Tarjeta unificada de estado del paquete de datos offline
                PackageItemStatus(
                    title = "Paquete completo offline (Mapa 3D, Rutas y POIs)",
                    isReady = isAllReady,
                    isDownloading = isDownloading,
                    percent = (downloadStatus as? MapDownloadStatus.Downloading)?.percent
                )

                if (downloadStatus is MapDownloadStatus.Failed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No se pudo descargar: ${downloadStatus.message}",
                        style = MaterialTheme.typography.bodySmall.copy(color = NavCoralRed)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isAllReady) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allowMeteredData,
                            onCheckedChange = { allowMeteredData = it },
                            colors = CheckboxDefaults.colors(checkedColor = NavAccentAmber)
                        )
                        Text(
                            text = "Permitir descarga con datos móviles (no solo WiFi)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDownloading) {
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar descarga")
                        }
                    } else {
                        Button(
                            onClick = { onStartDownload(allowMeteredData) },
                            modifier = Modifier.fillMaxWidth().testTag("btn_start_map_download")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (allowMeteredData) "Descargar ahora" else "Descargar por WiFi")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NavSurfaceBlue.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NavAccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAllReady)
                                "Todo listo: navegando 100% con datos reales de Cuba, sin conexión."
                            else
                                "Si no tienes internet ahora, también puedes copiar los archivos manualmente por USB — ver SETUP_MAPA_OFFLINE.md.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallDownloadProgress(status: MapDownloadStatus.Downloading) {
    val label = when (status.phase) {
        MapDataDownloadWorker.PHASE_DOWNLOAD_ZIP -> "Descargando paquete completo de Cuba…"
        MapDataDownloadWorker.PHASE_UNZIP -> "Descomprimiendo archivos offline…"
        else -> "Preparando descarga…"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NavSurfaceBlue
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${status.percent}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavCyan
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = NavCyan,
                trackColor = NavSurfaceBlue.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun PackageItemStatus(
    title: String,
    isReady: Boolean,
    isDownloading: Boolean,
    percent: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NavSurfaceBlue
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(NavCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = NavCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = when {
                            isReady -> "Listo, disponible sin conexión"
                            isDownloading -> "Descargando y extrayendo… ${percent ?: 0}%"
                            else -> "No descargado todavía"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isReady) NavEmerald else NavAccentAmber
                        )
                    )
                }
                if (isReady) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Listo", tint = NavEmerald, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
