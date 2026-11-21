package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        StageRecordEntity::class,
        UnlockedItemEntity::class,
        PowerUpUpgradeEntity::class,
        DailyMissionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MetroDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: MetroDatabase? = null

        fun getInstance(context: Context): MetroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MetroDatabase::class.java,
                    "metro_dash_3d.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
