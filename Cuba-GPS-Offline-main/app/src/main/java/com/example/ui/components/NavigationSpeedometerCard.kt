package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RouteResult
import com.example.location.GpsLocationData
import com.example.ui.theme.NavAccentAmber
import com.example.ui.theme.NavCoralRed
import com.example.ui.theme.NavCyan
import com.example.ui.theme.NavDeepBlue
import com.example.ui.theme.NavEmerald
import kotlin.math.roundToInt

@Composable
fun NavigationSpeedometerCard(
    locationData: GpsLocationData,
    activeRoute: RouteResult?,
    speedLimitKmh: Int = 100,
    modifier: Modifier = Modifier
) {
    val currentSpeed = locationData.speedKmh
    val isOverSpeed = currentSpeed > speedLimitKmh + 5

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("nav_speedometer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavDeepBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speedometer Gauge Block
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Limit Badge (Standard Red Circle with Number)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .border(3.dp, NavCoralRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$speedLimitKmh",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Current Live Speed
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${currentSpeed.roundToInt()}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isOverSpeed) NavCoralRed else Color.White,
                                fontSize = 32.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isOverSpeed) NavCoralRed else NavCyan,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Text(
                        text = if (locationData.isMockOrSimulated) "Simulación GPS" else "GPS Satelital",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (locationData.isMockOrSimulated) NavAccentAmber else NavEmerald
                        )
                    )
                }
            }

            // Route Trip Info: Remaining Distance & Time
            if (activeRoute != null) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${activeRoute.totalDistanceKm} km",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NavCyan,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDuration(activeRoute.estimatedTimeMinutes),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Alt: ${locationData.altitudeMeters.roundToInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                // Compass Mini Dial when not in active route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NavCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Brújula",
                            tint = NavCyan,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(locationData.bearing)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${locationData.bearing.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        "${hours} h ${mins} min"
    } else {
        "${mins} min"
    }
}
