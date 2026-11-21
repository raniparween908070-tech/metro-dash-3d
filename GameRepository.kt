package com.example.data

import com.example.data.local.DailyMissionEntity
import com.example.data.local.GameDao
import com.example.data.local.GameSettings
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PowerUpUpgradeEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.StageRecordEntity
import com.example.data.local.UnlockedItemEntity
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

sealed class PurchaseResult {
    data class Success(val message: String) : PurchaseResult()
    data class InsufficientFunds(val currency: CurrencyType, val needed: Int) : PurchaseResult()
    data class AlreadyOwned(val message: String) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

data class RunSummaryResult(
    val stageNumber: Int,
    val distanceMeters: Int,
    val score: Long,
    val coinsEarned: Int,
    val diamondsEarned: Int,
    val stageCompleted: Boolean,
    val stageBonusCoins: Int,
    val stageBonusDiamonds: Int,
    val unlockedNextStage: Int?,
    val isNewBestScore: Boolean
)

class GameRepository(
    private val dao: GameDao,
    private val settingsStore: SettingsDataStore
) {
    val settingsFlow: Flow<GameSettings> = settingsStore.settingsFlow

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map { raw ->
        val p = raw ?: PlayerProfileEntity()
        p.copy(
            coins = p.coins.coerceAtLeast(0),
            diamonds = p.diamonds.coerceAtLeast(0),
            currentStage = p.currentStage.coerceIn(1, StageGenerator.TOTAL_STAGES),
            highestUnlockedStage = p.highestUnlockedStage.coerceIn(1, StageGenerator.TOTAL_STAGES),
            hoverboardCharges = p.hoverboardCharges.coerceAtLeast(0)
        )
    }

    val stageRecordsMapFlow: Flow<Map<Int, StageRecordEntity>> = dao.observeAllStages().map { list ->
        list.associateBy { it.stageNumber }
    }

    val unlockedKeysFlow: Flow<Set<String>> = dao.observeUnlockedItems().map { list ->
        list.map { it.itemKey }.toSet()
    }

    val powerUpLevelsFlow: Flow<Map<PowerUpType, Int>> = dao.observePowerUps().map { list ->
        val map = list.associate { it.powerUpId to it.level.coerceIn(1, 5) }
        PowerUpType.entries.associateWith { type -> map[type.id] ?: 1 }
    }

    val missionsFlow: Flow<List<DailyMissionEntity>> = dao.observeMissions()

    val canClaimDailyRewardFlow: Flow<Boolean> = profileFlow.map { profile ->
        val today = LocalDate.now().toEpochDay()
        profile.lastDailyClaimEpochDay < today
    }

    val unClaimedCompletedMissionsCountFlow: Flow<Int> = missionsFlow.map { list ->
        list.count { !it.claimed && it.progress >= it.target }
    }

    val badgeAlertsFlow: Flow<Pair<Boolean, Int>> = combine(
        canClaimDailyRewardFlow,
        unClaimedCompletedMissionsCountFlow
    ) { dailyReady, missionsReady -> dailyReady to missionsReady }

    suspend fun ensureInitialized() {
        val existing = dao.getProfileOnce()
        val today = LocalDate.now().toEpochDay()
        if (existing == null) {
            val initial = computeSignedProfile(PlayerProfileEntity(lastMissionResetEpochDay = today))
            dao.upsertProfile(initial)
            seedStarterUnlocks()
            seedDailyMissions()
        } else {
            // Ensure starter unlocks exist
            val unlocked = dao.getUnlockedItemsOnce().map { it.itemKey }.toSet()
            if ("char:bolt" !in unlocked) seedStarterUnlocks()
            // Refresh daily missions if a new calendar day started
            if (existing.lastMissionResetEpochDay < today || dao.getMissionsOnce().isEmpty()) {
                seedDailyMissions()
                dao.upsertProfile(computeSignedProfile(existing.copy(lastMissionResetEpochDay = today)))
            }
        }
    }

    private suspend fun seedStarterUnlocks() {
        dao.unlockItem(UnlockedItemEntity("char:bolt"))
        dao.unlockItem(UnlockedItemEntity("outfit:default_classic"))
        dao.unlockItem(UnlockedItemEntity("board:metro_cruiser"))
        dao.upsertPowerUps(PowerUpType.entries.map { PowerUpUpgradeEntity(it.id, 1) })
    }

    private suspend fun seedDailyMissions() {
        val missions = listOf(
            DailyMissionEntity(
                id = 1,
                missionType = MissionType.DISTANCE.name,
                title = "Sprint 3,000 meters across metro lines",
                target = 3000,
                progress = 0,
                rewardCoins = 400,
                rewardDiamonds = 2
            ),
            DailyMissionEntity(
                id = 2,
                missionType = MissionType.COINS.name,
                title = "Collect 150 golden track coins",
                target = 150,
                progress = 0,
                rewardCoins = 350,
                rewardDiamonds = 2
            ),
            DailyMissionEntity(
                id = 3,
                missionType = MissionType.JUMPS.name,
                title = "Vault over 35 barriers or trains",
                target = 35,
                progress = 0,
                rewardCoins = 300,
                rewardDiamonds = 1
            ),
            DailyMissionEntity(
                id = 4,
                missionType = MissionType.POWERUPS.name,
                title = "Activate 5 track power-ups",
                target = 5,
                progress = 0,
                rewardCoins = 450,
                rewardDiamonds = 3
            ),
            DailyMissionEntity(
                id = 5,
                missionType = MissionType.STAGE_CLEAR.name,
                title = "Complete 1 full 5,000m Stage",
                target = 1,
                progress = 0,
                rewardCoins = 900,
                rewardDiamonds = 5
            )
        )
        dao.upsertMissions(missions)
    }

    private fun computeSignedProfile(profile: PlayerProfileEntity): PlayerProfileEntity {
        val hash = (profile.coins * 31 + profile.diamonds * 17 + profile.highestUnlockedStage * 13 + profile.bestScore.toInt())
        return profile.copy(integrityHash = hash)
    }

    suspend fun selectStage(stageNumber: Int) {
        val profile = dao.getProfileOnce() ?: return
        val clamped = stageNumber.coerceIn(1, profile.highestUnlockedStage)
        dao.upsertProfile(computeSignedProfile(profile.copy(currentStage = clamped)))
    }

    suspend fun consumeHoverboardCharge(): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        if (profile.hoverboardCharges <= 0) return false
        dao.upsertProfile(computeSignedProfile(profile.copy(hoverboardCharges = profile.hoverboardCharges - 1)))
        return true
    }

    suspend fun spendDiamondsForRevive(cost: Int = 5): Boolean {
        val profile = dao.getProfileOnce() ?: return false
        if (profile.diamonds < cost) return false
        dao.upsertProfile(computeSignedProfile(profile.copy(diamonds = profile.diamonds - cost)))
        return true
    }

    suspend fun recordRunEnd(
        stageNumber: Int,
        distanceMeters: Int,
        score: Long,
        coinsCollected: Int,
        diamondsCollected: Int,
        jumpsMade: Int,
        powerUpsActivated: Int,
        completedStage: Boolean
    ): RunSummaryResult {
        ensureInitialized()
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val stageConfig = StageGenerator.configForStage(stageNumber)
        val existingStage = dao.getStageOnce(stageNumber)
        val wasAlreadyCompleted = existingStage?.completed == true

        val bonusCoins = if (completedStage) {
            if (!wasAlreadyCompleted) stageConfig.rewardCoins else stageConfig.rewardCoins / 3
        } else 0
        val bonusDiamonds = if (completedStage) {
            if (!wasAlreadyCompleted) stageConfig.rewardDiamonds else 1
        } else 0

        val clampedDistance = distanceMeters.coerceIn(0, StageGenerator.STAGE_DISTANCE_METERS)
        val newBestStageDist = maxOf(existingStage?.bestDistance ?: 0, clampedDistance)
        val stars = when {
            completedStage && coinsCollected >= 120 -> 3
            completedStage && coinsCollected >= 60 -> 2
            completedStage -> 1
            else -> existingStage?.stars ?: 0
        }

        dao.upsertStage(
            StageRecordEntity(
                stageNumber = stageNumber,
                bestDistance = newBestStageDist,
                completed = wasAlreadyCompleted || completedStage,
                stars = maxOf(existingStage?.stars ?: 0, stars)
            )
        )

        val unlockedNext = if (completedStage && stageNumber < StageGenerator.TOTAL_STAGES &&
            stageNumber + 1 > profile.highestUnlockedStage
        ) {
            stageNumber + 1
        } else null

        val newHighestUnlocked = maxOf(
            profile.highestUnlockedStage,
            if (completedStage && stageNumber < StageGenerator.TOTAL_STAGES) stageNumber + 1 else profile.highestUnlockedStage
        )

        val nextSelectedStage = if (completedStage && stageNumber < StageGenerator.TOTAL_STAGES) {
            stageNumber + 1
        } else profile.currentStage

        val isNewBest = score > profile.bestScore
        val updatedProfile = profile.copy(
            coins = profile.coins + coinsCollected + bonusCoins,
            diamonds = profile.diamonds + diamondsCollected + bonusDiamonds,
            bestScore = maxOf(profile.bestScore, score),
            totalMeters = profile.totalMeters + distanceMeters,
            totalCoinsCollected = profile.totalCoinsCollected + coinsCollected,
            totalJumps = profile.totalJumps + jumpsMade,
            totalRuns = profile.totalRuns + 1,
            highestUnlockedStage = newHighestUnlocked,
            currentStage = nextSelectedStage
        )
        dao.upsertProfile(computeSignedProfile(updatedProfile))

        // Update daily missions
        val missions = dao.getMissionsOnce().map { m ->
            val delta = when (m.missionType) {
                MissionType.DISTANCE.name -> distanceMeters
                MissionType.COINS.name -> coinsCollected
                MissionType.JUMPS.name -> jumpsMade
                MissionType.POWERUPS.name -> powerUpsActivated
                MissionType.STAGE_CLEAR.name -> if (completedStage) 1 else 0
                else -> 0
            }
            m.copy(progress = (m.progress + delta).coerceAtMost(m.target))
        }
        dao.upsertMissions(missions)

        return RunSummaryResult(
            stageNumber = stageNumber,
            distanceMeters = clampedDistance,
            score = score,
            coinsEarned = coinsCollected,
            diamondsEarned = diamondsCollected,
            stageCompleted = completedStage,
            stageBonusCoins = bonusCoins,
            stageBonusDiamonds = bonusDiamonds,
            unlockedNextStage = unlockedNext,
            isNewBestScore = isNewBest
        )
    }

    suspend fun purchaseCharacter(character: CharacterDef): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        val key = "char:${character.id}"
        val owned = dao.getUnlockedItemsOnce().any { it.itemKey == key }
        if (owned) return PurchaseResult.AlreadyOwned("${character.name} already unlocked")

        val updated = when (character.currency) {
            CurrencyType.COINS -> {
                if (profile.coins < character.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.COINS, character.price - profile.coins)
                }
                profile.copy(coins = profile.coins - character.price, selectedCharacterId = character.id)
            }
            CurrencyType.DIAMONDS -> {
                if (profile.diamonds < character.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.DIAMONDS, character.price - profile.diamonds)
                }
                profile.copy(diamonds = profile.diamonds - character.price, selectedCharacterId = character.id)
            }
        }
        dao.unlockItem(UnlockedItemEntity(key))
        dao.upsertProfile(computeSignedProfile(updated))
        return PurchaseResult.Success("${character.name} unlocked and equipped!")
    }

    suspend fun equipCharacter(characterId: String) {
        val profile = dao.getProfileOnce() ?: return
        val owned = dao.getUnlockedItemsOnce().any { it.itemKey == "char:$characterId" }
        if (!owned) return
        // Reset outfit if current outfit belongs to a different character
        val currentOutfit = GameCatalog.outfitById(profile.selectedOutfitId)
        val nextOutfit = if (currentOutfit?.characterId == characterId) profile.selectedOutfitId else "default_classic"
        dao.upsertProfile(
            computeSignedProfile(
                profile.copy(selectedCharacterId = characterId, selectedOutfitId = nextOutfit)
            )
        )
    }

    suspend fun purchaseOutfit(outfit: OutfitDef): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        val key = "outfit:${outfit.id}"
        val owned = dao.getUnlockedItemsOnce().any { it.itemKey == key }
        if (owned) return PurchaseResult.AlreadyOwned("${outfit.name} already owned")

        val updated = when (outfit.currency) {
            CurrencyType.COINS -> {
                if (profile.coins < outfit.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.COINS, outfit.price - profile.coins)
                }
                profile.copy(coins = profile.coins - outfit.price)
            }
            CurrencyType.DIAMONDS -> {
                if (profile.diamonds < outfit.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.DIAMONDS, outfit.price - profile.diamonds)
                }
                profile.copy(diamonds = profile.diamonds - outfit.price)
            }
        }
        dao.unlockItem(UnlockedItemEntity(key))
        // Auto-equip if matching character is owned
        val charOwned = dao.getUnlockedItemsOnce().any { it.itemKey == "char:${outfit.characterId}" }
        val finalProfile = if (charOwned) {
            updated.copy(selectedCharacterId = outfit.characterId, selectedOutfitId = outfit.id)
        } else updated
        dao.upsertProfile(computeSignedProfile(finalProfile))
        return PurchaseResult.Success("${outfit.name} outfit unlocked!")
    }

    suspend fun equipOutfit(outfit: OutfitDef) {
        val profile = dao.getProfileOnce() ?: return
        val unlocked = dao.getUnlockedItemsOnce().map { it.itemKey }.toSet()
        if ("outfit:${outfit.id}" !in unlocked) return
        if ("char:${outfit.characterId}" !in unlocked) return
        dao.upsertProfile(
            computeSignedProfile(
                profile.copy(selectedCharacterId = outfit.characterId, selectedOutfitId = outfit.id)
            )
        )
    }

    suspend fun purchaseHoverboard(board: HoverboardDef): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        val key = "board:${board.id}"
        val owned = dao.getUnlockedItemsOnce().any { it.itemKey == key }
        if (owned) return PurchaseResult.AlreadyOwned("${board.name} already owned")

        val updated = when (board.currency) {
            CurrencyType.COINS -> {
                if (profile.coins < board.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.COINS, board.price - profile.coins)
                }
                profile.copy(coins = profile.coins - board.price, selectedHoverboardId = board.id)
            }
            CurrencyType.DIAMONDS -> {
                if (profile.diamonds < board.price) {
                    return PurchaseResult.InsufficientFunds(CurrencyType.DIAMONDS, board.price - profile.diamonds)
                }
                profile.copy(diamonds = profile.diamonds - board.price, selectedHoverboardId = board.id)
            }
        }
        dao.unlockItem(UnlockedItemEntity(key))
        dao.upsertProfile(computeSignedProfile(updated))
        return PurchaseResult.Success("${board.name} unlocked and equipped!")
    }

    suspend fun equipHoverboard(boardId: String) {
        val profile = dao.getProfileOnce() ?: return
        if (dao.getUnlockedItemsOnce().none { it.itemKey == "board:$boardId" }) return
        dao.upsertProfile(computeSignedProfile(profile.copy(selectedHoverboardId = boardId)))
    }

    suspend fun buyHoverboardCharges(quantity: Int = 3, coinCost: Int = 300): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        if (profile.coins < coinCost) {
            return PurchaseResult.InsufficientFunds(CurrencyType.COINS, coinCost - profile.coins)
        }
        dao.upsertProfile(
            computeSignedProfile(
                profile.copy(
                    coins = profile.coins - coinCost,
                    hoverboardCharges = profile.hoverboardCharges + quantity
                )
            )
        )
        return PurchaseResult.Success("Added +$quantity Hoverboard charges!")
    }

    suspend fun upgradePowerUp(type: PowerUpType): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        val currentLevel = dao.getPowerUpsOnce().firstOrNull { it.powerUpId == type.id }?.level ?: 1
        val cost = type.upgradeCostForNextLevel(currentLevel)
            ?: return PurchaseResult.AlreadyOwned("${type.title} is already MAX Lv 5!")
        if (profile.coins < cost) {
            return PurchaseResult.InsufficientFunds(CurrencyType.COINS, cost - profile.coins)
        }
        dao.upsertProfile(computeSignedProfile(profile.copy(coins = profile.coins - cost)))
        dao.upsertPowerUp(PowerUpUpgradeEntity(type.id, currentLevel + 1))
        return PurchaseResult.Success("${type.title} upgraded to Lv ${currentLevel + 1}!")
    }

    suspend fun exchangeDiamondsForCoins(pack: CurrencyPackDef): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        if (profile.diamonds < pack.diamondCost) {
            return PurchaseResult.InsufficientFunds(CurrencyType.DIAMONDS, pack.diamondCost - profile.diamonds)
        }
        dao.upsertProfile(
            computeSignedProfile(
                profile.copy(
                    diamonds = profile.diamonds - pack.diamondCost,
                    coins = profile.coins + pack.coinsGranted
                )
            )
        )
        return PurchaseResult.Success("+${pack.coinsGranted} Coins added!")
    }

    suspend fun claimDailyReward(): PurchaseResult {
        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        val today = LocalDate.now().toEpochDay()
        if (profile.lastDailyClaimEpochDay >= today) {
            return PurchaseResult.AlreadyOwned("Today's reward is already claimed! Come back tomorrow.")
        }
        val isConsecutive = profile.lastDailyClaimEpochDay == today - 1L
        val nextDayIndex = if (isConsecutive) ((profile.lastDailyClaimDayIndex % 7) + 1) else 1
        val reward = GameCatalog.dailyRewards[nextDayIndex - 1]
        val updated = profile.copy(
            coins = profile.coins + reward.coins,
            diamonds = profile.diamonds + reward.diamonds,
            hoverboardCharges = profile.hoverboardCharges + reward.hoverboards,
            lastDailyClaimDayIndex = nextDayIndex,
            dailyStreak = if (isConsecutive) profile.dailyStreak + 1 else 1,
            lastDailyClaimEpochDay = today
        )
        dao.upsertProfile(computeSignedProfile(updated))
        return PurchaseResult.Success(
            "Day $nextDayIndex Reward: +${reward.coins} Coins, +${reward.diamonds} Diamonds, +${reward.hoverboards} Boards!"
        )
    }

    suspend fun claimMissionReward(missionId: Int): PurchaseResult {
        val missions = dao.getMissionsOnce()
        val target = missions.firstOrNull { it.id == missionId }
            ?: return PurchaseResult.Error("Mission not found")
        if (target.claimed) return PurchaseResult.AlreadyOwned("Already claimed")
        if (target.progress < target.target) return PurchaseResult.Error("Mission not yet completed")

        val profile = dao.getProfileOnce() ?: return PurchaseResult.Error("Profile missing")
        dao.upsertProfile(
            computeSignedProfile(
                profile.copy(
                    coins = profile.coins + target.rewardCoins,
                    diamonds = profile.diamonds + target.rewardDiamonds
                )
            )
        )
        dao.upsertMissions(missions.map { if (it.id == missionId) it.copy(claimed = true) else it })
        return PurchaseResult.Success("Claimed +${target.rewardCoins} Coins & +${target.rewardDiamonds} Diamonds!")
    }

    suspend fun updatePlayerName(name: String) {
        val trimmed = name.trim().take(18).ifEmpty { "Metro Runner" }
        val profile = dao.getProfileOnce() ?: return
        dao.upsertProfile(computeSignedProfile(profile.copy(playerName = trimmed)))
    }

    suspend fun resetAllProgress() {
        dao.clearStages()
        dao.clearUnlockedItems()
        dao.clearPowerUps()
        dao.clearMissions()
        val today = LocalDate.now().toEpochDay()
        dao.upsertProfile(computeSignedProfile(PlayerProfileEntity(lastMissionResetEpochDay = today)))
        seedStarterUnlocks()
        seedDailyMissions()
    }

    // Settings delegations
    suspend fun setMusicEnabled(enabled: Boolean) = settingsStore.setMusicEnabled(enabled)
    suspend fun setSfxEnabled(enabled: Boolean) = settingsStore.setSfxEnabled(enabled)
    suspend fun setMusicVolume(volume: Float) = settingsStore.setMusicVolume(volume)
    suspend fun setSfxVolume(volume: Float) = settingsStore.setSfxVolume(volume)
    suspend fun setVibrationEnabled(enabled: Boolean) = settingsStore.setVibrationEnabled(enabled)
    suspend fun setGraphicsQuality(quality: GraphicsQuality) = settingsStore.setGraphicsQuality(quality)
    suspend fun setTargetFps(fps: Int) = settingsStore.setTargetFps(fps)
    suspend fun setLanguage(language: AppLanguage) = settingsStore.setLanguage(language)
}
