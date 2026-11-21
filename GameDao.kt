package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM stage_records ORDER BY stageNumber ASC")
    fun observeAllStages(): Flow<List<StageRecordEntity>>

    @Query("SELECT * FROM stage_records WHERE stageNumber = :stageNumber")
    suspend fun getStageOnce(stageNumber: Int): StageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStage(record: StageRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStages(records: List<StageRecordEntity>)

    @Query("SELECT * FROM unlocked_items")
    fun observeUnlockedItems(): Flow<List<UnlockedItemEntity>>

    @Query("SELECT * FROM unlocked_items")
    suspend fun getUnlockedItemsOnce(): List<UnlockedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockItem(item: UnlockedItemEntity)

    @Query("SELECT * FROM powerup_upgrades")
    fun observePowerUps(): Flow<List<PowerUpUpgradeEntity>>

    @Query("SELECT * FROM powerup_upgrades")
    suspend fun getPowerUpsOnce(): List<PowerUpUpgradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPowerUp(upgrade: PowerUpUpgradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPowerUps(upgrades: List<PowerUpUpgradeEntity>)

    @Query("SELECT * FROM daily_missions ORDER BY id ASC")
    fun observeMissions(): Flow<List<DailyMissionEntity>>

    @Query("SELECT * FROM daily_missions ORDER BY id ASC")
    suspend fun getMissionsOnce(): List<DailyMissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMissions(missions: List<DailyMissionEntity>)

    @Query("DELETE FROM stage_records")
    suspend fun clearStages()

    @Query("DELETE FROM unlocked_items")
    suspend fun clearUnlockedItems()

    @Query("DELETE FROM powerup_upgrades")
    suspend fun clearPowerUps()

    @Query("DELETE FROM daily_missions")
    suspend fun clearMissions()
}
