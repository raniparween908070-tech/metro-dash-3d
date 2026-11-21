package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
    COIN,
    DIAMOND,
    JUMP,
    SLIDE,
    LANE_SWITCH,
    POWERUP,
    SHIELD_BREAK,
    COLLISION,
    STAGE_COMPLETE,
    GAME_OVER,
    FOOTSTEP
}

class MetroSoundEngine(private val context: Context) {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var musicEnabled: Boolean = true
    @Volatile var sfxEnabled: Boolean = true
    @Volatile var musicVolume: Float = 0.7f
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var vibrationEnabled: Boolean = true

    private var musicJob: Job? = null
    private var musicTrack: AudioTrack? = null

    private val sfxCache: Map<SoundEffect, ShortArray> by lazy {
        mapOf(
            SoundEffect.COIN to generateToneSequence(listOf(987.77f to 0.055f, 1318.51f to 0.11f), 0.38f),
            SoundEffect.DIAMOND to generateToneSequence(
                listOf(1046.5f to 0.05f, 1318.5f to 0.05f, 1568.0f to 0.06f, 2093.0f to 0.15f),
                0.42f
            ),
            SoundEffect.JUMP to generateSweep(220f, 560f, 0.14f, 0.35f),
            SoundEffect.SLIDE to generateNoiseSweep(0.16f, 0.25f),
            SoundEffect.LANE_SWITCH to generateSweep(300f, 410f, 0.06f, 0.20f),
            SoundEffect.POWERUP to generateToneSequence(
                listOf(440f to 0.07f, 554.37f to 0.07f, 659.25f to 0.08f, 880f to 0.18f),
                0.45f
            ),
            SoundEffect.SHIELD_BREAK to generateSweep(720f, 180f, 0.22f, 0.45f),
            SoundEffect.COLLISION to generateImpact(0.28f, 0.6f),
            SoundEffect.STAGE_COMPLETE to generateToneSequence(
                listOf(523.25f to 0.10f, 659.25f to 0.10f, 783.99f to 0.12f, 1046.5f to 0.32f),
                0.50f
            ),
            SoundEffect.GAME_OVER to generateToneSequence(
                listOf(392.0f to 0.14f, 349.23f to 0.14f, 311.13f to 0.18f, 261.63f to 0.34f),
                0.45f
            ),
            SoundEffect.FOOTSTEP to generateImpact(0.035f, 0.12f)
        )
    }

    private val musicLoopPcm: ShortArray by lazy { generateArcadeMusicLoop() }

    fun playSfx(effect: SoundEffect) {
        if (!sfxEnabled || sfxVolume <= 0.01f) return
        val pcm = sfxCache[effect] ?: return
        scope.launch {
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setVolume(sfxVolume.coerceIn(0f, 1f))
                track.play()
                val durationMs = (pcm.size * 1000L / sampleRate) + 40L
                kotlinx.coroutines.delay(durationMs)
                track.stop()
                track.release()
            }
        }
    }

    fun startMusic() {
        if (!musicEnabled) return
        if (musicJob?.isActive == true) {
            updateMusicVolume()
            return
        }
        musicJob = scope.launch {
            runCatching {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                musicTrack = track
                track.setVolume((musicVolume * 0.45f).coerceIn(0f, 1f))
                track.play()

                val pcm = musicLoopPcm
                var offset = 0
                val chunk = 1024
                while (isActive && musicEnabled) {
                    val toWrite = minOf(chunk, pcm.size - offset)
                    track.write(pcm, offset, toWrite)
                    offset += toWrite
                    if (offset >= pcm.size) offset = 0
                }
                track.stop()
                track.release()
                musicTrack = null
            }
        }
    }

    fun updateMusicVolume() {
        runCatching {
            musicTrack?.setVolume((if (musicEnabled) musicVolume * 0.45f else 0f).coerceIn(0f, 1f))
        }
        if (!musicEnabled) {
            stopMusic()
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        runCatching {
            musicTrack?.stop()
            musicTrack?.release()
        }
        musicTrack = null
    }

    fun vibrate(durationMs: Long = 35L, amplitude: Int = 140) {
        if (!vibrationEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            )
        }
    }

    private fun generateToneSequence(notes: List<Pair<Float, Float>>, amp: Float): ShortArray {
        val totalSamples = notes.sumOf { (_, d) -> (d * sampleRate).toInt() }
        val out = ShortArray(totalSamples)
        var idx = 0
        for ((freq, dur) in notes) {
            val count = (dur * sampleRate).toInt()
            for (i in 0 until count) {
                val t = i.toFloat() / sampleRate
                val env = (1f - i.toFloat() / count) * exp(-2.5f * t)
                val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
                val sample = (wave * env * amp * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                if (idx < out.size) out[idx++] = sample.toShort()
            }
        }
        return out
    }

    private fun generateSweep(startFreq: Float, endFreq: Float, durationSec: Float, amp: Float): ShortArray {
        val count = (durationSec * sampleRate).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * p
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(PI * p).toFloat()
            out[i] = (sin(phase) * env * amp * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun generateNoiseSweep(durationSec: Float, amp: Float): ShortArray {
        val count = (durationSec * sampleRate).toInt()
        val out = ShortArray(count)
        var lp = 0f
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val n = (Random.nextFloat() * 2f - 1f)
            lp = lp * 0.72f + n * 0.28f
            val env = (1f - p) * sin(PI * p).toFloat()
            out[i] = (lp * env * amp * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun generateImpact(durationSec: Float, amp: Float): ShortArray {
        val count = (durationSec * sampleRate).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val freq = 135f * (1f - p * 0.7f)
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-9.0f * p)
            val rumble = sin(phase).toFloat() * 0.7f + (Random.nextFloat() * 2f - 1f) * 0.3f
            out[i] = (rumble * env * amp * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun generateArcadeMusicLoop(): ShortArray {
        // 4-bar upbeat cyber-metro synth bass + arpeggio at ~128 BPM (16th note = 0.117s)
        val stepSec = 0.117f
        val steps = 32
        val stepSamples = (stepSec * sampleRate).toInt()
        val out = ShortArray(steps * stepSamples)
        val bassNotes = floatArrayOf(
            110f, 110f, 110f, 110f, 130.81f, 130.81f, 146.83f, 146.83f,
            98f, 98f, 98f, 98f, 130.81f, 130.81f, 164.81f, 146.83f
        )
        val arpNotes = floatArrayOf(
            440f, 523.25f, 659.25f, 880f, 523.25f, 659.25f, 783.99f, 1046.5f,
            392f, 493.88f, 587.33f, 783.99f, 523.25f, 659.25f, 783.99f, 659.25f
        )
        for (s in 0 until steps) {
            val bassF = bassNotes[(s / 2) % bassNotes.size]
            val arpF = arpNotes[s % arpNotes.size]
            for (i in 0 until stepSamples) {
                val t = i.toFloat() / sampleRate
                val env = exp(-5.0f * (i.toFloat() / stepSamples))
                val bass = (sin(2.0 * PI * bassF * t) * 0.35 + sin(4.0 * PI * bassF * t) * 0.12)
                val arp = sin(2.0 * PI * arpF * t) * 0.22 * env
                val kick = if (s % 4 == 0 && i < stepSamples / 2) {
                    val kp = i.toFloat() / (stepSamples / 2)
                    sin(2.0 * PI * (120f * (1f - kp * 0.7f)) * t) * exp(-10f * kp) * 0.35
                } else 0.0
                val mix = ((bass + arp + kick) * 0.45 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[s * stepSamples + i] = mix.toShort()
            }
        }
        return out
    }
}
