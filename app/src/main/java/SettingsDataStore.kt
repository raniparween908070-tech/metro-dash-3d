package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.GraphicsQuality
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "metro_dash_settings")

data class GameSettings(
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.85f,
    val vibrationEnabled: Boolean = true,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val targetFps: Int = 60,
    val language: AppLanguage = AppLanguage.EN
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val LANGUAGE = stringPreferencesKey("language_code")
    }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        GameSettings(
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.7f,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            graphicsQuality = runCatching {
                GraphicsQuality.valueOf(prefs[Keys.GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name)
            }.getOrDefault(GraphicsQuality.HIGH),
            targetFps = prefs[Keys.TARGET_FPS] ?: 60,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.EN.code)
        )
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SFX_ENABLED] = enabled }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.SFX_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        context.settingsDataStore.edit { it[Keys.GRAPHICS_QUALITY] = quality.name }
    }

    suspend fun setTargetFps(fps: Int) {
        context.settingsDataStore.edit { it[Keys.TARGET_FPS] = if (fps <= 30) 30 else 60 }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }
}
