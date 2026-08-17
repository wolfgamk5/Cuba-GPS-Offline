package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedPlaceEntity::class, RouteHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NavDatabase : RoomDatabase() {

    abstract fun navDao(): NavDao

    companion object {
        @Volatile
        private var INSTANCE: NavDatabase? = null

        fun getDatabase(context: Context): NavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NavDatabase::class.java,
                    "cuba_nav_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
