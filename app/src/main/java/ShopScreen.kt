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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CharacterDef
import com.example.data.CurrencyPackDef
import com.example.data.CurrencyType
import com.example.data.GameCatalog
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import com.example.data.PowerUpType
import com.example.ui.MetroUiState
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDiamond
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary

@Composable
fun ShopScreen(
    state: MetroUiState,
    onBack: () -> Unit,
    onBuyCharacter: (CharacterDef) -> Unit,
    onEquipCharacter: (String) -> Unit,
    onBuyOutfit: (OutfitDef) -> Unit,
    onEquipOutfit: (OutfitDef) -> Unit,
    onBuyHoverboard: (HoverboardDef) -> Unit,
    onEquipHoverboard: (String) -> Unit,
    onBuyHoverboardCharges: () -> Unit,
    onUpgradePowerUp: (PowerUpType) -> Unit,
    onExchangeDiamonds: (CurrencyPackDef) -> Unit
) {
    val strings = Localization.get(state.settings.language)
    val tabs = listOf("Power-Ups", "Hoverboards", "Characters", "Outfits", "Coin Vault")
    var selectedTab by remember { mutableIntStateOf(0) }

    var pendingConfirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.shopTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MetroBgCard,
            contentColor = MetroCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("shop_tab_$idx")
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MetroGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Skateboarding, contentDescription = null, tint = MetroGold, modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Hoverboard Charges (${state.profile.hoverboardCharges})", style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                                        Text("+3 Instant Rail Shield Charges", style = MaterialTheme.typography.bodyMedium, color = MetroTextSecondary)
                                    }
                                }
                                Button(
                                    onClick = {
                                        pendingConfirm = "Buy +3 Hoverboard charges for 300 Coins?" to onBuyHoverboardCharges
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetroGold, contentColor = MetroBgDeep),
                                    modifier = Modifier.testTag("buy_board_charges_button")
                                ) {
                                    Text("300 Coins", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    items(PowerUpType.entries, key = { it.id }) { type ->
                        val level = state.powerUpLevels[type] ?: 1
                        val nextCost = type.upgradeCostForNextLevel(level)
                        val dur = type.durationForLevel(level)
                        ShopRowCard(
                            title = "${type.title} (Lv $level/5)",
                            subtitle = "${type.description} Active: ${dur}s",
                            accent = MetroCyan,
                            actionLabel = if (nextCost == null) "MAXED" else "Upgrade • ${nextCost}c",
                            actionEnabled = nextCost != null,
                            onAction = {
                                if (nextCost != null) {
                                    pendingConfirm = "Upgrade ${type.title} to Lv ${level + 1} for $nextCost Coins?" to {
                                        onUpgradePowerUp(type)
                                    }
                                }
                            }
                        )
                    }
                }
                1 -> {
                    items(GameCatalog.hoverboards, key = { it.id }) { board ->
                        val owned = "board:${board.id}" in state.unlockedKeys
                        val equipped = state.profile.selectedHoverboardId == board.id
                        val accent = Color(board.deckColor[0], board.deckColor[1], board.deckColor[2])
                        val priceLabel = if (board.currency == CurrencyType.DIAMONDS) "${board.price} Diamonds" else "${board.price} Coins"
                        ShopRowCard(
                            title = board.name,
                            subtitle = "${board.tagline} • Speed x${board.speedMultiplier}",
                            accent = accent,
                            actionLabel = when {
                                equipped -> strings.equipped
                                owned -> strings.equip
                                else -> "Buy • $priceLabel"
                            },
                            actionEnabled = !equipped,
                            isEquipped = equipped,
                            onAction = {
                                if (owned) {
                                    onEquipHoverboard(board.id)
                                } else {
                                    pendingConfirm = "Purchase ${board.name} for $priceLabel?" to {
                                        onBuyHoverboard(board)
                                    }
                                }
                            }
                        )
                    }
                }
                2 -> {
                    items(GameCatalog.characters, key = { it.id }) { charDef ->
                        val owned = "char:${charDef.id}" in state.unlockedKeys
                        val equipped = state.profile.selectedCharacterId == charDef.id
                        val accent = Color(charDef.primaryColor[0], charDef.primaryColor[1], charDef.primaryColor[2])
                        val priceLabel = if (charDef.currency == CurrencyType.DIAMONDS) "${charDef.price} Diamonds" else "${charDef.price} Coins"
                        ShopRowCard(
                            title = charDef.name,
                            subtitle = charDef.tagline,
                            accent = accent,
                            actionLabel = when {
                                equipped -> strings.equipped
                                owned -> strings.equip
                                else -> "Buy • $priceLabel"
                            },
                            actionEnabled = !equipped,
                            isEquipped = equipped,
                            onAction = {
                                if (owned) {
                                    onEquipCharacter(charDef.id)
                                } else {
                                    pendingConfirm = "Unlock ${charDef.name} for $priceLabel?" to {
                                        onBuyCharacter(charDef)
                                    }
                                }
                            }
                        )
                    }
                }
                3 -> {
                    items(GameCatalog.outfits, key = { it.id }) { outfit ->
                        val owned = "outfit:${outfit.id}" in state.unlockedKeys
                        val charOwned = "char:${outfit.characterId}" in state.unlockedKeys
                        val equipped = state.profile.selectedOutfitId == outfit.id
                        val accent = Color(outfit.primaryColor[0], outfit.primaryColor[1], outfit.primaryColor[2])
                        val priceLabel = if (outfit.currency == CurrencyType.DIAMONDS) "${outfit.price} Diamonds" else "${outfit.price} Coins"
                        ShopRowCard(
                            title = "${outfit.name} (${GameCatalog.characterById(outfit.characterId).name})",
                            subtitle = if (charOwned) "Custom 3D Runner Outfit" else "Requires ${GameCatalog.characterById(outfit.characterId).name} to equip",
                            accent = accent,
                            actionLabel = when {
                                equipped -> strings.equipped
                                owned -> strings.equip
                                else -> "Buy • $priceLabel"
                            },
                            actionEnabled = !equipped && (!owned || charOwned),
                            isEquipped = equipped,
                            onAction = {
                                if (owned) {
                                    onEquipOutfit(outfit)
                                } else {
                                    pendingConfirm = "Purchase ${outfit.name} outfit for $priceLabel?" to {
                                        onBuyOutfit(outfit)
                                    }
                                }
                            }
                        )
                    }
                }
                4 -> {
                    item {
                        Text(
                            "Exchange earned Diamonds for instant Coin Packs (in-game currency only — no real-money charges).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetroTextSecondary
                        )
                    }
                    items(GameCatalog.coinPacks, key = { it.id }) { pack ->
                        ShopRowCard(
                            title = pack.title,
                            subtitle = "Grants +%,d Track Coins immediately".format(pack.coinsGranted),
                            accent = MetroDiamond,
                            actionLabel = "${pack.diamondCost} Diamonds",
                            actionEnabled = true,
                            onAction = {
                                pendingConfirm = "Exchange ${pack.diamondCost} Diamonds for ${pack.coinsGranted} Coins?" to {
                                    onExchangeDiamonds(pack)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    pendingConfirm?.let { (message, action) ->
        ConfirmActionDialog(
            title = "Confirm Purchase",
            body = message,
            confirmLabel = "Confirm",
            confirmColor = MetroGold,
            onConfirm = {
                action()
                pendingConfirm = null
            },
            onDismiss = { pendingConfirm = null }
        )
    }
}

@Composable
private fun ShopRowCard(
    title: String,
    subtitle: String,
    accent: Color,
    actionLabel: String,
    actionEnabled: Boolean,
    isEquipped: Boolean = false,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetroBgCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isEquipped) MetroEmerald else MetroOutline, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f))
                        .border(1.5.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEquipped) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (isEquipped) MetroEmerald else accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MetroTextSecondary)
                }
            }
            if (isEquipped) {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(actionLabel, color = MetroEmerald)
                }
            } else {
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionLabel.contains("Equip")) MetroCyan else MetroGold,
                        contentColor = MetroBgDeep
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
