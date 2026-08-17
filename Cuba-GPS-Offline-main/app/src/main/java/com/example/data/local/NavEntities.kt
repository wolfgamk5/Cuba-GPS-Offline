package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.PoiCategory

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double,
    val categoryName: String,
    val isFavorite: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPoiCategory(): PoiCategory {
        return try {
            PoiCategory.valueOf(categoryName)
        } catch (e: Exception) {
            PoiCategory.TURISMO
        }
    }
}

@Entity(tableName = "route_history")
data class RouteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originName: String,
    val originLat: Double,
    val originLon: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLon: Double,
    val distanceKm: Double,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
