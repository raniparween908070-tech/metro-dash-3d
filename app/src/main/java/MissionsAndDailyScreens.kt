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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.ui.MetroUiState
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDiamond
import com.example.ui.theme.MetroDiamondGlow
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary

@Composable
fun DailyRewardsScreen(
    state: MetroUiState,
    onBack: () -> Unit,
    onClaimDaily: () -> Unit
) {
    val strings = Localization.get(state.settings.language)
    val claimedUpTo = state.profile.lastDailyClaimDayIndex
    val nextDay = if (state.canClaimDailyReward) {
        (claimedUpTo % 7) + 1
    } else -1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.dailyRewardsTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MetroDiamondGlow.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Login Streak: ${state.profile.dailyStreak} Days",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MetroGold
                            )
                            Text(
                                text = if (state.canClaimDailyReward) "Today's reward is ready to claim!" else "Today's reward claimed. Come back tomorrow!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetroTextSecondary
                            )
                        }
                        Button(
                            onClick = onClaimDaily,
                            enabled = state.canClaimDailyReward,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MetroGold,
                                contentColor = MetroBgDeep
                            ),
                            modifier = Modifier.testTag("claim_daily_button")
                        ) {
                            Text(if (state.canClaimDailyReward) strings.claim else strings.claimed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(GameCatalog.dailyRewards, key = { it.dayIndex }) { day ->
                val isClaimed = day.dayIndex <= claimedUpTo && !(state.canClaimDailyReward && claimedUpTo == 7)
                val isCurrent = day.dayIndex == nextDay
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent -> MetroGold
                                isClaimed -> MetroEmerald
                                else -> MetroOutline
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) MetroGold.copy(alpha = 0.2f) else MetroBgDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isClaimed) Icons.Filled.CheckCircle else Icons.Filled.CardGiftcard,
                                    contentDescription = null,
                                    tint = when {
                                        isClaimed -> MetroEmerald
                                        isCurrent -> MetroGold
                                        else -> MetroCyan
                                    }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DAY ${day.dayIndex}" + if (isCurrent) " • TODAY" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrent) MetroGold else MetroTextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = MetroGold, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${day.coins}", style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Diamond, contentDescription = null, tint = MetroDiamond, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${day.diamonds}", style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Skateboarding, contentDescription = null, tint = MetroCyan, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("+${day.hoverboards}", style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
                                    }
                                }
                            }
                        }

                        Text(
                            text = when {
                                isClaimed -> "CLAIMED"
                                isCurrent -> "READY"
                                else -> "UPCOMING"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                isClaimed -> MetroEmerald
                                isCurrent -> MetroGold
                                else -> MetroTextSecondary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionsScreen(
    state: MetroUiState,
    onBack: () -> Unit,
    onClaimMission: (Int) -> Unit
) {
    val strings = Localization.get(state.settings.language)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.missionsTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Complete daily objectives across any of the 1,000 stages to earn bonus Coins and Diamonds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetroTextSecondary
                )
            }
            items(state.missions, key = { it.id }) { mission ->
                val done = mission.progress >= mission.target
                val frac = (mission.progress / mission.target.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when {
                                mission.claimed -> MetroOutline
                                done -> MetroEmerald
                                else -> MetroOutline
                            },
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(mission.title, style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                                Text(
                                    text = "Reward: +${mission.rewardCoins} Coins • +${mission.rewardDiamonds} Diamonds",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetroGold
                                )
                            }
                            when {
                                mission.claimed -> {
                                    Text(strings.claimed, style = MaterialTheme.typography.labelLarge, color = MetroEmerald)
                                }
                                done -> {
                                    Button(
                                        onClick = { onClaimMission(mission.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MetroEmerald, contentColor = MetroBgDeep),
                                        modifier = Modifier.testTag("claim_mission_${mission.id}")
                                    ) {
                                        Text(strings.claim, fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "${mission.progress} / ${mission.target}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MetroCyan
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { frac },
                            color = if (done) MetroEmerald else MetroCyan,
                            trackColor = MetroBgDeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}
