package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RouteStep
import com.example.data.TurnType
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald

@Composable
fun NavigationTopBanner(
    currentStep: RouteStep?,
    distanceToNextTurnMeters: Double,
    isVoiceMuted: Boolean,
    onToggleVoice: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentStep == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nav_top_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavDeepBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turn Direction Icon Circle
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = NavCyan.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, NavCyan)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTurnIcon(currentStep.turnType),
                        contentDescription = currentStep.turnType.name,
                        tint = NavCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Turn Instruction & Road Name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatDistance(distanceToNextTurnMeters),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentStep.instruction,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = NavAccentAmber,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentStep.roadName.isNotBlank()) {
                    Text(
                        text = currentStep.roadName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons (TTS Mute/Unmute & Stop Nav)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isVoiceMuted) Color(0x33FF5252) else Color(0x332EC4B6),
                            CircleShape
                        )
                        .testTag("toggle_voice_btn")
                ) {
                    Icon(
                        imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Voz",
                        tint = if (isVoiceMuted) Color(0xFFFF5252) else NavEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStopNavigation,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33E63946), CircleShape)
                        .testTag("stop_navigation_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Detener",
                        tint = Color(0xFFE63946),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
    return when {
        meters >= 1000 -> "En ${(meters / 1000.0 * 10).toInt() / 10.0} km"
        meters > 50 -> "En ${(meters / 10).toInt() * 10} m"
        else -> "Ahora"
    }
}
