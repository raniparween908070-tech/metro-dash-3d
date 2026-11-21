package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameCatalog
import com.example.data.StageGenerator
import com.example.ui.MetroUiState
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroBgElevated
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDiamond
import com.example.ui.theme.MetroDiamondGlow
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1100)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(MetroBgDeep, Color(0xFF0E1D3E), MetroBgDeep))
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_metro_hero),
            contentDescription = "Metro Dash 3D background",
            contentScale = ContentScale.Crop,
            alpha = 0.25f,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_launcher_art),
                contentDescription = "Metro Dash 3D emblem",
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, MetroCyan, RoundedCornerShape(28.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "METRO DASH 3D",
                style = MaterialTheme.typography.displayLarge,
                color = MetroCyan
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "1,000 STAGES • 5,000,000 METERS",
                style = MaterialTheme.typography.labelLarge,
                color = MetroGold
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                color = MetroCyan,
                trackColor = MetroBgCard,
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun HomeScreen(
    state: MetroUiState,
    onPlayStage: (Int) -> Unit,
    onOpenStages: () -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenDailyRewards: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onUpdateName: (String) -> Unit
) {
    val strings = Localization.get(state.settings.language)
    val stageConfig = remember(state.profile.currentStage) {
        StageGenerator.configForStage(state.profile.currentStage)
    }
    val selectedChar = remember(state.profile.selectedCharacterId) {
        GameCatalog.characterById(state.profile.selectedCharacterId)
    }
    val selectedBoard = remember(state.profile.selectedHoverboardId) {
        GameCatalog.hoverboardById(state.profile.selectedHoverboardId)
    }
    val completedStagesCount = remember(state.stageRecords) {
        state.stageRecords.values.count { it.completed }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "play_pulse")
    val playScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MetroBgDeep, Color(0xFF0C1631), MetroBgDeep)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Profile & Currency Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(MetroBgCard)
                        .border(1.dp, MetroOutline, RoundedCornerShape(50))
                        .clickable { showEditNameDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("player_profile_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Color(
                                    selectedChar.primaryColor[0],
                                    selectedChar.primaryColor[1],
                                    selectedChar.primaryColor[2]
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsRun,
                            contentDescription = "Runner avatar",
                            tint = MetroBgDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.profile.playerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MetroTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedChar.name} • ${selectedBoard.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetroCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit runner name",
                        tint = MetroTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyPill(
                        iconColor = MetroGold,
                        value = formatCompactNumber(state.profile.coins),
                        onClick = onOpenShop
                    )
                    CurrencyPill(
                        iconColor = MetroDiamond,
                        value = formatCompactNumber(state.profile.diamonds),
                        isDiamond = true,
                        onClick = onOpenShop
                    )
                }
            }

            // Hero Key Art Banner + Game Title + Best Score
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, MetroCyan.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_metro_hero),
                    contentDescription = "Metro Dash 3D railway skyline",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x33070C18),
                                    Color(0xAA070C18),
                                    Color(0xEE070C18)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MetroBgDeep.copy(alpha = 0.78f))
                                .border(1.dp, MetroGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stageConfig.biome.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MetroGold
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(MetroBgDeep.copy(alpha = 0.75f))
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = MetroTextPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "METRO DASH 3D",
                            style = MaterialTheme.typography.displayLarge,
                            color = MetroTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatChip(
                                icon = Icons.Filled.EmojiEvents,
                                label = "${strings.bestScore}: %,d".format(state.profile.bestScore),
                                tint = MetroGold
                            )
                            StatChip(
                                icon = Icons.Filled.AutoAwesome,
                                label = "$completedStagesCount / 1000 CLEARED",
                                tint = MetroEmerald
                            )
                        }
                    }
                }
            }

            // Active Stage Card & Giant PLAY CTA
            Card(
                colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MetroOutline, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${strings.currentStage} ${state.profile.currentStage} / 1000",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MetroCyan
                            )
                            Text(
                                text = "${stageConfig.biome.title} • ${strings.stageGoalLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetroTextSecondary
                            )
                        }
                        CurrencyPill(
                            iconColor = MetroCyan,
                            value = "${state.profile.hoverboardCharges}",
                            isBoard = true,
                            onClick = onOpenShop
                        )
                    }

                    val bestDist = state.stageRecords[state.profile.currentStage]?.bestDistance ?: 0
                    val progress = (bestDist / StageGenerator.STAGE_DISTANCE_METERS.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stage Best: %,d m".format(bestDist),
                                style = MaterialTheme.typography.labelSmall,
                                color = MetroTextSecondary
                            )
                            Text(
                                text = "Reward: +${stageConfig.rewardCoins} Coins, +${stageConfig.rewardDiamonds} Diamonds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MetroGold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            color = MetroCyan,
                            trackColor = MetroBgDeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayStage(state.profile.currentStage) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MetroCyan,
                                contentColor = MetroBgDeep
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 15.dp),
                            modifier = Modifier
                                .weight(1.45f)
                                .scale(playScale)
                                .testTag("play_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.playStage} ${state.profile.currentStage}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenStages,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 15.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stages_button")
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = MetroGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.stagesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MetroTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Primary Feature Dock: Runners, Shop, Missions, Daily Rewards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeActionTile(
                    icon = Icons.Filled.DirectionsRun,
                    label = strings.charactersTitle,
                    accent = MetroCyan,
                    onClick = onOpenCharacters,
                    modifier = Modifier.weight(1f).testTag("characters_button")
                )
                HomeActionTile(
                    icon = Icons.Filled.ShoppingBag,
                    label = strings.shopTitle.replace("METRO ", ""),
                    accent = MetroGold,
                    onClick = onOpenShop,
                    modifier = Modifier.weight(1f).testTag("shop_button")
                )
                HomeActionTile(
                    icon = Icons.Filled.TaskAlt,
                    label = "MISSIONS",
                    accent = MetroEmerald,
                    badgeCount = state.readyMissionsCount,
                    onClick = onOpenMissions,
                    modifier = Modifier.weight(1f).testTag("missions_button")
                )
                HomeActionTile(
                    icon = Icons.Filled.CalendarMonth,
                    label = "DAILY",
                    accent = MetroDiamondGlow,
                    showDot = state.canClaimDailyReward,
                    onClick = onOpenDailyRewards,
                    modifier = Modifier.weight(1f).testTag("daily_button")
                )
            }

            // Career Summary + Support Links
            Card(
                colors = CardDefaults.cardColors(containerColor = MetroBgCard.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MetroOutline, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL CAMPAIGN DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetroTextSecondary
                        )
                        Text(
                            text = "%,d / 5,000,000 m".format(state.profile.totalMeters),
                            style = MaterialTheme.typography.titleMedium,
                            color = MetroTextPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenHelp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = strings.helpTitle,
                                tint = MetroCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Help", color = MetroTextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onOpenPrivacy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("privacy_button")
                        ) {
                            Icon(
                                Icons.Filled.PrivacyTip,
                                contentDescription = strings.privacyTitle,
                                tint = MetroTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Privacy", color = MetroTextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(state.profile.playerName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = MetroBgCard,
            title = { Text("Runner Profile", color = MetroTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Total Runs: ${state.profile.totalRuns} • Jumps: ${state.profile.totalJumps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MetroTextSecondary
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 18) nameInput = it },
                        label = { Text("Runner Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateName(nameInput)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = MetroTextPrimary)
                }
            }
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MetroTextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    badgeCount: Int = 0,
    showDot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MetroBgElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(86.dp)
            .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(containerColor = MetroGold, contentColor = MetroBgDeep) {
                            Text("$badgeCount")
                        }
                    } else if (showDot) {
                        Badge(containerColor = MetroGold)
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MetroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
