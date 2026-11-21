package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MetroSoundEngine
import com.example.data.CharacterDef
import com.example.data.CurrencyPackDef
import com.example.data.CurrencyType
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.data.GraphicsQuality
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import com.example.data.PowerUpType
import com.example.data.PurchaseResult
import com.example.data.RunSummaryResult
import com.example.data.local.DailyMissionEntity
import com.example.data.local.GameSettings
import com.example.data.local.MetroDatabase
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.StageRecordEntity
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MetroUiState(
    val isReady: Boolean = false,
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val stageRecords: Map<Int, StageRecordEntity> = emptyMap(),
    val unlockedKeys: Set<String> = setOf("char:bolt", "outfit:default_classic", "board:metro_cruiser"),
    val powerUpLevels: Map<PowerUpType, Int> = PowerUpType.entries.associateWith { 1 },
    val missions: List<DailyMissionEntity> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val canClaimDailyReward: Boolean = true,
    val readyMissionsCount: Int = 0
)

class MetroViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MetroDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    val repository = GameRepository(database.gameDao(), settingsStore)
    val soundEngine = MetroSoundEngine(application)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _lastRunSummary = MutableStateFlow<RunSummaryResult?>(null)
    val lastRunSummary: StateFlow<RunSummaryResult?> = _lastRunSummary

    private val coreDataFlow = combine(
        repository.profileFlow,
        repository.stageRecordsMapFlow,
        repository.unlockedKeysFlow,
        repository.powerUpLevelsFlow,
        repository.missionsFlow
    ) { profile, stages, unlocked, powerUps, missions ->
        CoreBundle(profile, stages, unlocked, powerUps, missions)
    }

    val uiState: StateFlow<MetroUiState> = combine(
        coreDataFlow,
        repository.settingsFlow,
        repository.badgeAlertsFlow
    ) { core, settings, badges ->
        soundEngine.musicEnabled = settings.musicEnabled
        soundEngine.sfxEnabled = settings.sfxEnabled
        soundEngine.musicVolume = settings.musicVolume
        soundEngine.sfxVolume = settings.sfxVolume
        soundEngine.vibrationEnabled = settings.vibrationEnabled
        soundEngine.updateMusicVolume()

        MetroUiState(
            isReady = true,
            profile = core.profile,
            stageRecords = core.stages,
            unlockedKeys = core.unlocked,
            powerUpLevels = core.powerUps,
            missions = core.missions,
            settings = settings,
            canClaimDailyReward = badges.first,
            readyMissionsCount = badges.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetroUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun startMenuMusic() {
        soundEngine.startMusic()
    }

    fun stopMusic() {
        soundEngine.stopMusic()
    }

    fun selectStage(stageNumber: Int) {
        viewModelScope.launch {
            repository.selectStage(stageNumber)
        }
    }

    fun clearLastRunSummary() {
        _lastRunSummary.value = null
    }

    fun finishRun(
        stageNumber: Int,
        distanceMeters: Int,
        score: Long,
        coinsCollected: Int,
        diamondsCollected: Int,
        jumpsMade: Int,
        powerUpsActivated: Int,
        completedStage: Boolean
    ) {
        viewModelScope.launch {
            val summary = repository.recordRunEnd(
                stageNumber = stageNumber,
                distanceMeters = distanceMeters,
                score = score,
                coinsCollected = coinsCollected,
                diamondsCollected = diamondsCollected,
                jumpsMade = jumpsMade,
                powerUpsActivated = powerUpsActivated,
                completedStage = completedStage
            )
            _lastRunSummary.value = summary
        }
    }

    suspend fun tryConsumeHoverboard(): Boolean = repository.consumeHoverboardCharge()

    suspend fun trySpendReviveDiamonds(): Boolean = repository.spendDiamondsForRevive(5)

    fun buyCharacter(character: CharacterDef) {
        viewModelScope.launch {
            handlePurchaseResult(repository.purchaseCharacter(character))
        }
    }

    fun equipCharacter(characterId: String) {
        viewModelScope.launch {
            repository.equipCharacter(characterId)
            emitMessage("${GameCatalog.characterById(characterId).name} equipped!")
        }
    }

    fun buyOutfit(outfit: OutfitDef) {
        viewModelScope.launch {
            handlePurchaseResult(repository.purchaseOutfit(outfit))
        }
    }

    fun equipOutfit(outfit: OutfitDef) {
        viewModelScope.launch {
            repository.equipOutfit(outfit)
            emitMessage("${outfit.name} outfit equipped!")
        }
    }

    fun buyHoverboard(board: HoverboardDef) {
        viewModelScope.launch {
            handlePurchaseResult(repository.purchaseHoverboard(board))
        }
    }

    fun equipHoverboard(boardId: String) {
        viewModelScope.launch {
            repository.equipHoverboard(boardId)
            emitMessage("${GameCatalog.hoverboardById(boardId).name} equipped!")
        }
    }

    fun buyHoverboardCharges() {
        viewModelScope.launch {
            handlePurchaseResult(repository.buyHoverboardCharges(quantity = 3, coinCost = 300))
        }
    }

    fun upgradePowerUp(type: PowerUpType) {
        viewModelScope.launch {
            handlePurchaseResult(repository.upgradePowerUp(type))
        }
    }

    fun exchangeDiamonds(pack: CurrencyPackDef) {
        viewModelScope.launch {
            handlePurchaseResult(repository.exchangeDiamondsForCoins(pack))
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            handlePurchaseResult(repository.claimDailyReward())
        }
    }

    fun claimMission(missionId: Int) {
        viewModelScope.launch {
            handlePurchaseResult(repository.claimMissionReward(missionId))
        }
    }

    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            repository.updatePlayerName(name)
            emitMessage("Runner name updated!")
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMusicEnabled(enabled)
            if (enabled) soundEngine.startMusic() else soundEngine.stopMusic()
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSfxEnabled(enabled) }
    }

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch { repository.setMusicVolume(volume) }
    }

    fun setSfxVolume(volume: Float) {
        viewModelScope.launch { repository.setSfxVolume(volume) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        viewModelScope.launch { repository.setGraphicsQuality(quality) }
    }

    fun setTargetFps(fps: Int) {
        viewModelScope.launch { repository.setTargetFps(fps) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            emitMessage("All game progress reset to Stage 1.")
        }
    }

    private fun handlePurchaseResult(result: PurchaseResult) {
        when (result) {
            is PurchaseResult.Success -> emitMessage(result.message)
            is PurchaseResult.AlreadyOwned -> emitMessage(result.message)
            is PurchaseResult.InsufficientFunds -> {
                val unit = if (result.currency == CurrencyType.COINS) "Coins" else "Diamonds"
                emitMessage("Need ${result.needed} more $unit!")
            }
            is PurchaseResult.Error -> emitMessage(result.message)
        }
    }

    private fun emitMessage(msg: String) {
        _messages.tryEmit(msg)
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stopMusic()
    }

    private data class CoreBundle(
        val profile: PlayerProfileEntity,
        val stages: Map<Int, StageRecordEntity>,
        val unlocked: Set<String>,
        val powerUps: Map<PowerUpType, Int>,
        val missions: List<DailyMissionEntity>
    )
}
