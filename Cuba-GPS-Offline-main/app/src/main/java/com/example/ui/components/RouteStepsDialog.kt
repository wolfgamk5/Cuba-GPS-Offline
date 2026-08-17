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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RouteResult
import com.example.data.RouteStep
import com.example.data.TurnType
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import com.example.ui.theme.NavSurfaceBlue
import kotlin.math.roundToInt

@Composable
fun RouteStepsDialog(
    route: RouteResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(580.dp)
                .testTag("route_steps_dialog"),
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
                    Column {
                        Text(
                            text = "Hoja de Ruta e Indicaciones",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavCyan
                            )
                        )
                        Text(
                            text = "${route.totalDistanceKm} km • ${route.estimatedTimeMinutes / 60} h ${route.estimatedTimeMinutes % 60} min",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NavAccentAmber
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(route.steps) { index, step ->
                        StepItem(index = index + 1, step = step)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(index: Int, step: RouteStep) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                    .size(36.dp)
                    .background(NavCyan.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTurnIcon(step.turnType),
                    contentDescription = null,
                    tint = NavCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (step.roadName.isNotBlank()) {
                    Text(
                        text = step.roadName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NavAccentAmber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatDistance(step.distanceMeters),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

private fun getTurnIcon(type: TurnType): ImageVector {
    return when (type) {
        TurnType.START -> Icons.Default.NearMe
        TurnType.STRAIGHT -> Icons.Default.Straight
        TurnType.SLIGHT_RIGHT -> Icons.Default.TurnSlightRight
        TurnType.RIGHT -> Icons.Default.TurnRight
        TurnType.SHARP_RIGHT -> Icons.Default.TurnSharpRight
        TurnType.SLIGHT_LEFT -> Icons.Default.TurnSlightLeft
        TurnType.LEFT -> Icons.Default.TurnLeft
        TurnType.SHARP_LEFT -> Icons.Default.TurnSharpLeft
        TurnType.UTURN -> Icons.Default.Undo
        TurnType.ROUNDABOUT -> Icons.Default.Refresh
        TurnType.DESTINATION -> Icons.Default.Flag
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${(meters / 1000.0 * 10).roundToInt() / 10.0} km"
    } else {
        "${meters.roundToInt()} m"
    }
}
