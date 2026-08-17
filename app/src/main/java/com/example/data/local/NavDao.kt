package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NavDao {

    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Delete
    suspend fun deleteSavedPlace(place: SavedPlaceEntity)

    @Query("SELECT * FROM route_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentRoutes(): Flow<List<RouteHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteHistory(history: RouteHistoryEntity)

    @Query("DELETE FROM route_history")
    suspend fun clearRouteHistory()
}
