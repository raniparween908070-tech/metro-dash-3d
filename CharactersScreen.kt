package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.CharacterDef
import com.example.data.CurrencyType
import com.example.data.GameCatalog
import com.example.data.OutfitDef
import com.example.game3d.CharacterPreviewGLView
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
fun CharactersScreen(
    state: MetroUiState,
    onBack: () -> Unit,
    onBuyCharacter: (CharacterDef) -> Unit,
    onEquipCharacter: (String) -> Unit,
    onBuyOutfit: (OutfitDef) -> Unit,
    onEquipOutfit: (OutfitDef) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = Localization.get(state.settings.language)

    var previewChar by remember(state.profile.selectedCharacterId) {
        mutableStateOf(GameCatalog.characterById(state.profile.selectedCharacterId))
    }
    var previewOutfit by remember(state.profile.selectedOutfitId) {
        mutableStateOf(GameCatalog.outfitById(state.profile.selectedOutfitId))
    }
    var showBoard by remember { mutableStateOf(false) }
    var confirmChar by remember { mutableStateOf<CharacterDef?>(null) }
    var confirmOutfit by remember { mutableStateOf<OutfitDef?>(null) }

    val selectedBoard = remember(state.profile.selectedHoverboardId) {
        GameCatalog.hoverboardById(state.profile.selectedHoverboardId)
    }

    val previewGl = remember { CharacterPreviewGLView(context) }
    previewGl.currentCharacter = previewChar
    previewGl.currentOutfit = if (previewOutfit?.characterId == previewChar.id) previewOutfit else null
    previewGl.currentHoverboard = selectedBoard
    previewGl.showHoverboard = showBoard

    DisposableEffect(lifecycleOwner, previewGl) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> previewGl.onResume()
                Lifecycle.Event.ON_PAUSE -> previewGl.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            previewGl.onPause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.charactersTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3D Rotating Character Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, MetroCyan.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_character_stage),
                    contentDescription = "Character showcase hangar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { previewGl },
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    FilterChip(
                        selected = showBoard,
                        onClick = { showBoard = !showBoard },
                        label = { Text("Board") },
                        leadingIcon = {
                            Icon(Icons.Filled.Skateboarding, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(MetroBgDeep.copy(alpha = 0.72f))
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(previewChar.name, style = MaterialTheme.typography.headlineMedium, color = MetroCyan)
                    Text(previewChar.tagline, style = MaterialTheme.typography.bodyMedium, color = MetroTextSecondary)
                }
            }

            // Character Action Bar
            val isCharOwned = "char:${previewChar.id}" in state.unlockedKeys
            val isCharEquipped = state.profile.selectedCharacterId == previewChar.id
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    isCharEquipped -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MetroEmerald)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.equipped, color = MetroEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                    isCharOwned -> {
                        Button(
                            onClick = { onEquipCharacter(previewChar.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep),
                            modifier = Modifier.fillMaxWidth().testTag("equip_character_button")
                        ) {
                            Text("${strings.equip} ${previewChar.name}", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        val isDiamond = previewChar.currency == CurrencyType.DIAMONDS
                        Button(
                            onClick = { confirmChar = previewChar },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDiamond) MetroDiamond else MetroGold,
                                contentColor = MetroBgDeep
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("buy_character_button")
                        ) {
                            Icon(
                                if (isDiamond) Icons.Filled.Diamond else Icons.Filled.MonetizationOn,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Unlock ${previewChar.name} • ${previewChar.price} ${if (isDiamond) "Diamonds" else "Coins"}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Characters Roster
            Text("ALL RUNNERS", style = MaterialTheme.typography.titleMedium, color = MetroTextSecondary)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(GameCatalog.characters, key = { it.id }) { charDef ->
                    val owned = "char:${charDef.id}" in state.unlockedKeys
                    val selected = previewChar.id == charDef.id
                    val equipped = state.profile.selectedCharacterId == charDef.id
                    val accent = Color(charDef.primaryColor[0], charDef.primaryColor[1], charDef.primaryColor[2])

                    Card(
                        onClick = {
                            previewChar = charDef
                            previewOutfit = null
                        },
                        colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .width(136.dp)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MetroCyan else MetroOutline,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("character_card_${charDef.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Text(charDef.name, style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                            Text(
                                text = when {
                                    equipped -> "EQUIPPED"
                                    owned -> "OWNED"
                                    charDef.currency == CurrencyType.DIAMONDS -> "${charDef.price} Diamonds"
                                    else -> "${charDef.price} Coins"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    equipped -> MetroEmerald
                                    owned -> MetroCyan
                                    else -> MetroGold
                                }
                            )
                        }
                    }
                }
            }

            // Outfits for Selected Character
            val matchingOutfits = GameCatalog.outfits.filter { it.characterId == previewChar.id }
            Text("OUTFITS FOR ${previewChar.name.uppercase()}", style = MaterialTheme.typography.titleMedium, color = MetroTextSecondary)
            if (matchingOutfits.isEmpty()) {
                Text(
                    "Standard signature gear equipped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MetroTextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchingOutfits.forEach { outfit ->
                        val owned = "outfit:${outfit.id}" in state.unlockedKeys
                        val equipped = state.profile.selectedOutfitId == outfit.id
                        val primaryCol = Color(outfit.primaryColor[0], outfit.primaryColor[1], outfit.primaryColor[2])
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MetroBgCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (equipped) MetroEmerald else MetroOutline, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(primaryCol)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(outfit.name, style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                                        Text(
                                            if (owned) "Unlocked" else "${outfit.price} ${if (outfit.currency == CurrencyType.DIAMONDS) "Diamonds" else "Coins"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MetroTextSecondary
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { previewOutfit = outfit }) {
                                        Text("Preview", color = MetroCyan)
                                    }
                                    when {
                                        equipped -> Text("ACTIVE", color = MetroEmerald, style = MaterialTheme.typography.labelLarge)
                                        owned -> Button(
                                            onClick = { onEquipOutfit(outfit) },
                                            enabled = isCharOwned,
                                            colors = ButtonDefaults.buttonColors(containerColor = MetroCyan, contentColor = MetroBgDeep)
                                        ) { Text(strings.equip) }
                                        else -> Button(
                                            onClick = { confirmOutfit = outfit },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetroGold, contentColor = MetroBgDeep)
                                        ) { Text(strings.buy) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    confirmChar?.let { c ->
        ConfirmActionDialog(
            title = "Unlock ${c.name}?",
            body = "Purchase ${c.name} (${c.tagline}) for ${c.price} ${if (c.currency == CurrencyType.DIAMONDS) "Diamonds" else "Coins"}?",
            confirmLabel = "Unlock",
            confirmColor = MetroGold,
            onConfirm = {
                onBuyCharacter(c)
                confirmChar = null
            },
            onDismiss = { confirmChar = null }
        )
    }

    confirmOutfit?.let { o ->
        ConfirmActionDialog(
            title = "Unlock ${o.name}?",
            body = "Purchase ${o.name} outfit for ${o.price} ${if (o.currency == CurrencyType.DIAMONDS) "Diamonds" else "Coins"}?",
            confirmLabel = "Buy",
            confirmColor = MetroGold,
            onConfirm = {
                onBuyOutfit(o)
                confirmOutfit = null
            },
            onDismiss = { confirmOutfit = null }
        )
    }
}
