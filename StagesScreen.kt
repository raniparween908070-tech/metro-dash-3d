package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.StageGenerator
import com.example.ui.MetroUiState
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroBgElevated
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary
import kotlinx.coroutines.launch

@Composable
fun StagesScreen(
    state: MetroUiState,
    onBack: () -> Unit,
    onSelectAndPlayStage: (Int) -> Unit
) {
    val strings = Localization.get(state.settings.language)
    val scope = rememberCoroutineScope()
    var selectedSector by remember { mutableIntStateOf(((state.profile.currentStage - 1) / 125).coerceIn(0, 7)) }
    var jumpInput by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    val startStage = selectedSector * 125 + 1
    val endStage = (startStage + 124).coerceAtMost(StageGenerator.TOTAL_STAGES)
    val stageNumbers = remember(selectedSector) { (startStage..endStage).toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.stagesTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        // Campaign summary + Jump to Stage
        Card(
            colors = CardDefaults.cardColors(containerColor = MetroBgCard),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, MetroOutline, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Unlocked: ${state.profile.highestUnlockedStage} / 1000",
                        style = MaterialTheme.typography.titleMedium,
                        color = MetroCyan
                    )
                    Text(
                        text = "5,000m / Stage • 5,000,000m Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetroGold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jumpInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) jumpInput = it },
                        label = { Text("Jump to Stage (1-1000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("stage_jump_input")
                    )
                    Button(
                        onClick = {
                            val num = jumpInput.toIntOrNull()?.coerceIn(1, 1000) ?: return@Button
                            selectedSector = (num - 1) / 125
                            val indexInSector = (num - 1) % 125
                            scope.launch { gridState.animateScrollToItem(indexInSector) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep),
                        modifier = Modifier.testTag("stage_jump_button")
                    ) {
                        Text("Go", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 8 Sector Filter Chips (1-125 ... 876-1000)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(8) { sector ->
                val s0 = sector * 125 + 1
                val s1 = (s0 + 124).coerceAtMost(1000)
                FilterChip(
                    selected = selectedSector == sector,
                    onClick = { selectedSector = sector },
                    label = { Text("$s0-$s1") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetroCyan,
                        selectedLabelColor = MetroBgDeep,
                        containerColor = MetroBgCard,
                        labelColor = MetroTextPrimary
                    )
                )
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(stageNumbers.size, key = { stageNumbers[it] }) { idx ->
                val stageNum = stageNumbers[idx]
                val config = remember(stageNum) { StageGenerator.configForStage(stageNum) }
                val record = state.stageRecords[stageNum]
                val unlocked = stageNum <= state.profile.highestUnlockedStage
                val isSelected = stageNum == state.profile.currentStage
                val biomeColor = Color(
                    config.biome.railGlow[0],
                    config.biome.railGlow[1],
                    config.biome.railGlow[2]
                )

                Card(
                    onClick = { if (unlocked) onSelectAndPlayStage(stageNum) },
                    enabled = unlocked,
                    colors = CardDefaults.cardColors(
                        containerColor = if (unlocked) MetroBgCard else MetroBgElevated.copy(alpha = 0.45f),
                        disabledContainerColor = MetroBgElevated.copy(alpha = 0.40f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> MetroCyan
                                record?.completed == true -> MetroEmerald.copy(alpha = 0.65f)
                                unlocked -> MetroOutline
                                else -> MetroOutline.copy(alpha = 0.35f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("stage_card_$stageNum")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STAGE $stageNum",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (unlocked) MetroTextPrimary else MetroTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            when {
                                !unlocked -> Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = MetroTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                record?.completed == true -> Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = MetroEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                else -> Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Playable",
                                    tint = MetroCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(biomeColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = config.biome.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MetroTextSecondary,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "Goal: 5,000m • +${config.rewardCoins}c",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unlocked) MetroGold else MetroTextSecondary
                        )

                        val bestDist = record?.bestDistance ?: 0
                        LinearProgressIndicator(
                            progress = { (bestDist / 5000f).coerceIn(0f, 1f) },
                            color = if (record?.completed == true) MetroEmerald else MetroCyan,
                            trackColor = MetroBgDeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            val stars = record?.stars ?: 0
                            repeat(3) { starIdx ->
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (starIdx < stars) MetroGold else MetroOutline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
