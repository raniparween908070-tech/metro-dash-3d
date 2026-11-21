package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GraphicsQuality
import com.example.ui.MetroUiState
import com.example.ui.MetroViewModel
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.Localization
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroCoral
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary

@Composable
fun SettingsScreen(
    state: MetroUiState,
    viewModel: MetroViewModel,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val strings = Localization.get(state.settings.language)
    var showResetConfirm1 by remember { mutableStateOf(false) }
    var showResetConfirm2 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.settingsTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Audio & Haptics
            SettingsSectionCard("AUDIO & HAPTICS") {
                SettingToggleRow(
                    label = "Background Music",
                    checked = state.settings.musicEnabled,
                    onCheckedChange = { viewModel.setMusicEnabled(it) },
                    testTag = "music_switch"
                )
                if (state.settings.musicEnabled) {
                    Text("Music Volume: ${(state.settings.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
                    Slider(
                        value = state.settings.musicVolume,
                        onValueChange = { viewModel.setMusicVolume(it) },
                        colors = SliderDefaults.colors(thumbColor = MetroCyan, activeTrackColor = MetroCyan)
                    )
                }
                SettingToggleRow(
                    label = "Sound Effects",
                    checked = state.settings.sfxEnabled,
                    onCheckedChange = { viewModel.setSfxEnabled(it) },
                    testTag = "sfx_switch"
                )
                if (state.settings.sfxEnabled) {
                    Text("SFX Volume: ${(state.settings.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MetroTextSecondary)
                    Slider(
                        value = state.settings.sfxVolume,
                        onValueChange = { viewModel.setSfxVolume(it) },
                        colors = SliderDefaults.colors(thumbColor = MetroGold, activeTrackColor = MetroGold)
                    )
                }
                SettingToggleRow(
                    label = "Vibration Feedback",
                    checked = state.settings.vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                    testTag = "vibration_switch"
                )
            }

            // 3D Graphics & Performance
            SettingsSectionCard("3D GRAPHICS & PERFORMANCE") {
                Text("Graphics Quality", style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsQuality.entries.forEach { q ->
                        FilterChip(
                            selected = state.settings.graphicsQuality == q,
                            onClick = { viewModel.setGraphicsQuality(q) },
                            label = { Text(q.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetroCyan,
                                selectedLabelColor = MetroBgDeep
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Frame Rate Target", style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60).forEach { fps ->
                        FilterChip(
                            selected = state.settings.targetFps == fps,
                            onClick = { viewModel.setTargetFps(fps) },
                            label = { Text("$fps FPS") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetroCyan,
                                selectedLabelColor = MetroBgDeep
                            )
                        )
                    }
                }
            }

            // Language Selection
            SettingsSectionCard("LANGUAGE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.take(3).forEach { lang ->
                        FilterChip(
                            selected = state.settings.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = { Text(lang.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetroGold,
                                selectedLabelColor = MetroBgDeep
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.drop(3).forEach { lang ->
                        FilterChip(
                            selected = state.settings.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = { Text(lang.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetroGold,
                                selectedLabelColor = MetroBgDeep
                            )
                        )
                    }
                }
            }

            // Support, Privacy & Reset
            SettingsSectionCard("SUPPORT & DATA") {
                OutlinedButton(
                    onClick = onOpenHelp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MetroCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.helpTitle, color = MetroTextPrimary)
                }
                OutlinedButton(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MetroCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.privacyTitle, color = MetroTextPrimary)
                }
                Button(
                    onClick = { showResetConfirm1 = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MetroCoral, contentColor = MetroTextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("reset_progress_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.resetProgress, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showResetConfirm1) {
        ConfirmActionDialog(
            title = "Reset All Game Progress?",
            body = "This will erase your Coins, Diamonds, unlocked Characters, Outfits, Hoverboards, Power-up levels, and 1,000-stage progress.",
            confirmLabel = "Continue",
            confirmColor = MetroCoral,
            onConfirm = {
                showResetConfirm1 = false
                showResetConfirm2 = true
            },
            onDismiss = { showResetConfirm1 = false }
        )
    }

    if (showResetConfirm2) {
        ConfirmActionDialog(
            title = "Final Confirmation",
            body = "Are you completely sure? All saved local data will be permanently deleted and restored to Stage 1.",
            confirmLabel = "Erase Everything",
            confirmColor = MetroCoral,
            onConfirm = {
                showResetConfirm2 = false
                viewModel.resetProgress()
            },
            onDismiss = { showResetConfirm2 = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetroBgCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MetroOutline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MetroCyan)
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MetroTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun PrivacyPolicyScreen(
    state: MetroUiState,
    onBack: () -> Unit
) {
    val strings = Localization.get(state.settings.language)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.privacyTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoSectionCard(
                title = "1. Data Collection & Personal Information",
                body = "METRO DASH 3D does not collect, sell, or transmit personal data (such as your name, email, location, contacts, or device identifiers) to external servers. Any Runner Name you enter is stored strictly on your device."
            )
            InfoSectionCard(
                title = "2. Local Storage",
                body = "All game progress—including Coins, Diamonds, unlocked Stages (1–1000), Best Scores, Characters, Outfits, Hoverboards, Power-up upgrades, Daily Rewards, Missions, and Audio/Graphics Settings—is saved locally on your device using Android Room (SQLite) and Jetpack DataStore."
            )
            InfoSectionCard(
                title = "3. Advertising & Analytics Practices",
                body = "This version of METRO DASH 3D contains zero third-party advertising SDKs, zero tracking pixels, and zero remote analytics telemetry."
            )
            InfoSectionCard(
                title = "4. Third-Party Services & Permissions",
                body = "The app does not integrate third-party social logins or real-money payment gateways. It uses only the standard Android VIBRATE permission for optional haptic collision feedback, which you can disable at any time in Settings."
            )
            InfoSectionCard(
                title = "5. Data Deletion",
                body = "You have complete control over your saved data. You can permanently erase all local game data at any time via Settings -> Reset Progress, or by uninstalling the application."
            )
            InfoSectionCard(
                title = "6. Contact Information",
                body = "For privacy questions or support inquiries, contact our team at support@metrodashrunner.app."
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun HelpSupportScreen(
    state: MetroUiState,
    onBack: () -> Unit
) {
    val strings = Localization.get(state.settings.language)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroBgDeep)
            .navigationBarsPadding()
    ) {
        MetroTopBar(
            title = strings.helpTitle,
            coins = state.profile.coins,
            diamonds = state.profile.diamonds,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoSectionCard(
                title = "How to Play & Swipe Controls",
                body = "• Swipe LEFT / RIGHT: Switch across the 3 railway lanes.\n• Swipe UP: Jump over low barriers or launch off glowing ramps onto train roofs.\n• Swipe DOWN: Slide under overhead signal gates or fast-slam downward mid-air.\n• DOUBLE TAP or Tap Board Icon: Activate your equipped Hoverboard."
            )
            InfoSectionCard(
                title = "Coins & Diamonds",
                body = "Collect spinning gold Coins along the tracks to upgrade Power-ups and buy Runners, Outfits, and Hoverboards. Collect rare glowing Diamonds to unlock elite characters, exchange for Coin Packs, or revive instantly after a crash."
            )
            InfoSectionCard(
                title = "All 6 Power-Ups",
                body = "• Coin Magnet: Pulls nearby coins automatically.\n• Turbo Jetpack: Flies high above all trains on a sky-coin trail.\n• Super Sneakers: Boosts jump height to clear tall obstacles.\n• 2X Multiplier: Doubles all distance and pickup score.\n• Energy Shield: Absorbs one collision impact.\n• Hoverboard: Rides on a neon rail deck and protects from one crash."
            )
            InfoSectionCard(
                title = "1,000 Stages & Unlocking",
                body = "Every stage from Stage 1 to Stage 1,000 requires 5,000 meters (5,000,000 meters total). Completing a stage unlocks the next stage and awards bonus Coins and Diamonds. You can replay any unlocked stage via the 1000 STAGES screen."
            )
            InfoSectionCard(
                title = "Troubleshooting & Contact Support",
                body = "If frame rate drops on older devices, open Settings and switch Graphics Quality to Medium/Low or set Target FPS to 30 FPS. All progress saves automatically at the end of every run.\n\nSupport Email: support@metrodashrunner.app"
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoSectionCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetroBgCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MetroOutline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MetroCyan)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MetroTextSecondary)
        }
    }
}
