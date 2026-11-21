package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Metro Runner",
    val coins: Int = 500,
    val diamonds: Int = 10,
    val bestScore: Long = 0L,
    val totalMeters: Long = 0L,
    val totalCoinsCollected: Long = 0L,
    val totalJumps: Long = 0L,
    val totalRuns: Int = 0,
    val currentStage: Int = 1,
    val highestUnlockedStage: Int = 1,
    val selectedCharacterId: String = "bolt",
    val selectedOutfitId: String = "default_classic",
    val selectedHoverboardId: String = "metro_cruiser",
    val hoverboardCharges: Int = 3,
    val lastDailyClaimDayIndex: Int = 0,
    val dailyStreak: Int = 0,
    val lastDailyClaimEpochDay: Long = 0L,
    val lastMissionResetEpochDay: Long = 0L,
    val integrityHash: Int = 0
)

@Entity(tableName = "stage_records")
data class StageRecordEntity(
    @PrimaryKey val stageNumber: Int,
    val bestDistance: Int = 0,
    val completed: Boolean = false,
    val stars: Int = 0
)

@Entity(tableName = "unlocked_items")
data class UnlockedItemEntity(
    @PrimaryKey val itemKey: String, // e.g., "char:bolt", "outfit:bolt_voltage", "board:ion_blade"
    val unlockedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "powerup_upgrades")
data class PowerUpUpgradeEntity(
    @PrimaryKey val powerUpId: String,
    val level: Int = 1
)

@Entity(tableName = "daily_missions")
data class DailyMissionEntity(
    @PrimaryKey val id: Int,
    val missionType: String,
    val title: String,
    val target: Int,
    val progress: Int = 0,
    val rewardCoins: Int,
    val rewardDiamonds: Int,
    val claimed: Boolean = false
)
