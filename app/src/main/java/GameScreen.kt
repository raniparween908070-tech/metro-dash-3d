package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.SoundEffect
import com.example.data.GameCatalog
import com.example.data.RunSummaryResult
import com.example.data.StageGenerator
import com.example.game3d.GameWorldSimulation
import com.example.game3d.HudSnapshot
import com.example.game3d.MetroGLSurfaceView
import com.example.game3d.SimulationEvents
import com.example.ui.MetroUiState
import com.example.ui.MetroViewModel
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroCoral
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDiamond
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    stageNumber: Int,
    state: MetroUiState,
    viewModel: MetroViewModel,
    runSummary: RunSummaryResult?,
    onNavigateNextStage: (Int) -> Unit,
    onExitHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val strings = Localization.get(state.settings.language)

    val stageConfig = remember(stageNumber) { StageGenerator.configForStage(stageNumber) }
    val character = remember(state.profile.selectedCharacterId) {
        GameCatalog.characterById(state.profile.selectedCharacterId)
    }
    val outfit = remember(state.profile.selectedOutfitId) {
        GameCatalog.outfitById(state.profile.selectedOutfitId)
    }
    val hoverboard = remember(state.profile.selectedHoverboardId) {
        GameCatalog.hoverboardById(state.profile.selectedHoverboardId)
    }

    var hud by remember(stageNumber) {
        mutableStateOf(
            HudSnapshot(
                stageNumber = stageNumber,
                biomeName = stageConfig.biome.title,
                targetMeters = stageConfig.targetDistanceMeters,
                remainingMeters = stageConfig.targetDistanceMeters
            )
        )
    }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isStageComplete by remember { mutableStateOf(false) }

    val simulation = remember(stageNumber) {
        viewModel.clearLastRunSummary()
        GameWorldSimulation(
            stageConfig = stageConfig,
            powerUpLevels = state.powerUpLevels,
            hoverboardSpeedBonus = hoverboard.speedMultiplier,
            events = object : SimulationEvents {
                override fun onSound(effect: SoundEffect) {
                    viewModel.soundEngine.playSfx(effect)
                }

                override fun onHaptic(durationMs: Long, amplitude: Int) {
                    viewModel.soundEngine.vibrate(durationMs, amplitude)
                }

                override fun onGameOver(
                    distanceMeters: Int,
                    score: Long,
                    coins: Int,
                    diamonds: Int,
                    jumps: Int,
                    powerUps: Int
                ) {
                    isGameOver = true
                    viewModel.finishRun(
                        stageNumber = stageNumber,
                        distanceMeters = distanceMeters,
                        score = score,
                        coinsCollected = coins,
                        diamondsCollected = diamonds,
                        jumpsMade = jumps,
                        powerUpsActivated = powerUps,
                        completedStage = false
                    )
                }

                override fun onStageCompleted(
                    distanceMeters: Int,
                    score: Long,
                    coins: Int,
                    diamonds: Int,
                    jumps: Int,
                    powerUps: Int
                ) {
                    isStageComplete = true
                    viewModel.finishRun(
                        stageNumber = stageNumber,
                        distanceMeters = distanceMeters,
                        score = score,
                        coinsCollected = coins,
                        diamondsCollected = diamonds,
                        jumpsMade = jumps,
                        powerUpsActivated = powerUps,
                        completedStage = true
                    )
                }
            }
        )
    }

    fun tryActivateHoverboard() {
        if (isPaused || isGameOver || isStageComplete) return
        scope.launch {
            if (viewModel.tryConsumeHoverboard()) {
                simulation.activateHoverboard()
            }
        }
    }

    val glView = remember(stageNumber) {
        MetroGLSurfaceView(
            context = context,
            simulation = simulation,
            stageConfig = stageConfig,
            graphicsQuality = state.settings.graphicsQuality,
            targetFps = state.settings.targetFps,
            character = character,
            outfit = outfit,
            hoverboard = hoverboard,
            onDoubleTapHoverboard = { tryActivateHoverboard() },
            onHudTick = { snap -> hud = snap }
        )
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    glView.onPause()
                    if (!isGameOver && !isStageComplete) {
                        simulation.isPaused = true
                        isPaused = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    BackHandler {
        if (isGameOver || isStageComplete) {
            onExitHome()
        } else {
            isPaused = !isPaused
            simulation.isPaused = isPaused
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Real 3D OpenGL ES Canvas
        AndroidView(
            factory = { glView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_gl_surface")
        )

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage & Distance Pill
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MetroBgDeep.copy(alpha = 0.78f))
                        .border(1.dp, MetroCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${strings.currentStage} ${hud.stageNumber} • ${hud.biomeName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetroCyan
                    )
                    Text(
                        text = "%,d / 5,000 m  (Left: %,d m)".format(hud.distanceMeters, hud.remainingMeters),
                        style = MaterialTheme.typography.labelLarge,
                        color = MetroTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (hud.distanceMeters / 5000f).coerceIn(0f, 1f) },
                        color = MetroCyan,
                        trackColor = MetroBgCard,
                        modifier = Modifier
                            .width(190.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }

                // Pause Button
                IconButton(
                    onClick = {
                        if (!isGameOver && !isStageComplete) {
                            isPaused = true
                            simulation.isPaused = true
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(MetroBgDeep.copy(alpha = 0.8f))
                        .border(1.dp, MetroOutline, CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = MetroTextPrimary)
                }
            }

            // Score + Currencies Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MetroBgDeep.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "%,d".format(hud.score),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MetroTextPrimary
                    )
                    if (hud.score2xRemaining > 0f) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "2X",
                            style = MaterialTheme.typography.labelLarge,
                            color = MetroGold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyPill(iconColor = MetroGold, value = "${hud.coins}")
                    CurrencyPill(iconColor = MetroDiamond, value = "${hud.diamonds}", isDiamond = true)
                }
            }

            // Active Power-Up Timers Row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hud.magnetRemaining > 0f) PowerUpChip("MAG", hud.magnetRemaining, MetroCoral)
                if (hud.jetpackRemaining > 0f) PowerUpChip("JET", hud.jetpackRemaining, MetroGold)
                if (hud.sneakersRemaining > 0f) PowerUpChip("JMP", hud.sneakersRemaining, MetroEmerald)
                if (hud.score2xRemaining > 0f) PowerUpChip("2X", hud.score2xRemaining, Color(0xFFD946EF))
                if (hud.shieldRemaining > 0f) PowerUpChip("SHD", hud.shieldRemaining, MetroCyan)
                if (hud.hoverboardRemaining > 0f) PowerUpChip("BRD", hud.hoverboardRemaining, MetroGold)
            }
        }

        // Bottom Controls Hint & Hoverboard Quick Button
        if (!isPaused && !isGameOver && !isStageComplete) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MetroBgDeep.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SWIPE: ◀ ▶ Lane  ▲ Jump  ▼ Slide  •  2x Tap: Board",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetroTextSecondary
                    )
                }

                Button(
                    onClick = { tryActivateHoverboard() },
                    enabled = state.profile.hoverboardCharges > 0 && hud.hoverboardRemaining <= 0f,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MetroGold,
                        contentColor = MetroBgDeep
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hoverboard_button")
                ) {
                    Icon(Icons.Filled.Skateboarding, contentDescription = "Activate Hoverboard")
                    Spacer(Modifier.width(4.dp))
                    Text("${state.profile.hoverboardCharges}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Pause Modal
        if (isPaused && !isGameOver && !isStageComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB050914)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .border(1.5.dp, MetroCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(strings.pause, style = MaterialTheme.typography.headlineLarge, color = MetroCyan)
                        Text(
                            "Stage ${hud.stageNumber} • ${hud.distanceMeters} / 5,000 m",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MetroTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Music", color = MetroTextPrimary)
                            Switch(
                                checked = state.settings.musicEnabled,
                                onCheckedChange = { viewModel.setMusicEnabled(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sound Effects", color = MetroTextPrimary)
                            Switch(
                                checked = state.settings.sfxEnabled,
                                onCheckedChange = { viewModel.setSfxEnabled(it) }
                            )
                        }
                        Button(
                            onClick = {
                                isPaused = false
                                simulation.isPaused = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep),
                            modifier = Modifier.fillMaxWidth().testTag("resume_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.resume, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                isGameOver = false
                                isStageComplete = false
                                viewModel.clearLastRunSummary()
                                simulation.resetWorld()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("restart_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = MetroGold)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restart, color = MetroTextPrimary)
                        }
                        OutlinedButton(
                            onClick = onExitHome,
                            modifier = Modifier.fillMaxWidth().testTag("quit_home_button")
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = null, tint = MetroTextSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.home, color = MetroTextPrimary)
                        }
                    }
                }
            }
        }

        // Game Over Overlay
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC8050914)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .border(1.5.dp, MetroCoral, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(strings.gameOver, style = MaterialTheme.typography.headlineLarge, color = MetroCoral)
                        if (runSummary?.isNewBestScore == true) {
                            Text("NEW BEST SCORE!", style = MaterialTheme.typography.labelLarge, color = MetroGold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RunMetric("DISTANCE", "%,d m".format(hud.distanceMeters), MetroCyan)
                            RunMetric("SCORE", "%,d".format(hud.score), MetroTextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RunMetric("COINS", "+${hud.coins}", MetroGold)
                            RunMetric("DIAMONDS", "+${hud.diamonds}", MetroDiamond)
                        }

                        if (state.profile.diamonds >= 5) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (viewModel.trySpendReviveDiamonds()) {
                                            isGameOver = false
                                            viewModel.clearLastRunSummary()
                                            simulation.reviveAfterCrash()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetroDiamond, contentColor = MetroBgDeep),
                                modifier = Modifier.fillMaxWidth().testTag("revive_button")
                            ) {
                                Icon(Icons.Filled.Diamond, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Revive Now (5 Diamonds)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                isGameOver = false
                                viewModel.clearLastRunSummary()
                                simulation.resetWorld()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep),
                            modifier = Modifier.fillMaxWidth().testTag("retry_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restart, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onExitHome,
                            modifier = Modifier.fillMaxWidth().testTag("gameover_home_button")
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = null, tint = MetroTextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.home, color = MetroTextPrimary)
                        }
                    }
                }
            }
        }

        // Stage Complete Celebration Overlay
        if (isStageComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC060D1E)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .border(2.dp, MetroGold, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MetroGold, modifier = Modifier.size(48.dp))
                        Text(strings.stageComplete, style = MaterialTheme.typography.headlineLarge, color = MetroGold)
                        Text(
                            "Stage $stageNumber • 5,000 / 5,000 Meters!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MetroCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val stars = when {
                                hud.coins >= 120 -> 3
                                hud.coins >= 60 -> 2
                                else -> 1
                            }
                            repeat(3) { idx ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Star",
                                    tint = if (idx < stars) MetroGold else MetroOutline,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        val bonusCoins = runSummary?.stageBonusCoins ?: stageConfig.rewardCoins
                        val bonusDiamonds = runSummary?.stageBonusDiamonds ?: stageConfig.rewardDiamonds
                        Text(
                            text = "Stage Bonus: +$bonusCoins Coins • +$bonusDiamonds Diamonds",
                            style = MaterialTheme.typography.labelLarge,
                            color = MetroEmerald
                        )
                        if (stageNumber < StageGenerator.TOTAL_STAGES) {
                            Text(
                                text = "Stage ${stageNumber + 1} Unlocked!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MetroTextPrimary
                            )
                            Button(
                                onClick = { onNavigateNextStage(stageNumber + 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep),
                                modifier = Modifier.fillMaxWidth().testTag("next_stage_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("${strings.nextStage} (${stageNumber + 1})", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onExitHome,
                            modifier = Modifier.fillMaxWidth().testTag("stage_complete_home_button")
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = null, tint = MetroTextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.home, color = MetroTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerUpChip(code: String, seconds: Float, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MetroBgDeep.copy(alpha = 0.85f))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = code, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text = "$code ${String.format("%.1fs", seconds)}",
            style = MaterialTheme.typography.labelSmall,
            color = MetroTextPrimary
        )
    }
}

@Composable
private fun RunMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
    }
}
