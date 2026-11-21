package com.example.game3d

import com.example.audio.SoundEffect
import com.example.data.PowerUpType
import com.example.data.StageConfig
import com.example.data.StageGenerator
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

enum class ObstacleKind {
    LOW_BARRIER,      // Jump over
    HIGH_GATE,        // Slide under
    FULL_BARRIER,     // Change lane
    PARKED_TRAIN,     // Tall train (change lane or climb via ramp / jetpack / sneakers)
    MOVING_TRAIN,     // Fast oncoming train
    RAMP              // Climb onto train roof
}

enum class CollectibleKind {
    COIN,
    DIAMOND,
    POWERUP
}

enum class SegmentStyle {
    OPEN_STATION,
    BRIDGE,
    TUNNEL
}

class TrackSegment {
    var zCenter: Float = 0f
    var style: SegmentStyle = SegmentStyle.OPEN_STATION
    var hasLeftPlatform: Boolean = true
    var hasRightPlatform: Boolean = true
    var leftBuildingHeight: Float = 10f
    var rightBuildingHeight: Float = 12f
    var active: Boolean = false
}

class ObstacleEntity {
    var active: Boolean = false
    var kind: ObstacleKind = ObstacleKind.LOW_BARRIER
    var lane: Int = 0
    var z: Float = 0f
    var length: Float = 2f
    var width: Float = 1.8f
    var height: Float = 1.1f
    var clearanceBottomY: Float = 0f // >0 for overhead gate you slide under
    var extraSpeed: Float = 0f
    var hitOnce: Boolean = false
}

class CollectibleEntity {
    var active: Boolean = false
    var kind: CollectibleKind = CollectibleKind.COIN
    var powerUpType: PowerUpType = PowerUpType.MAGNET
    var lane: Int = 0
    var x: Float = 0f
    var y: Float = 0.8f
    var z: Float = 0f
    var spinPhase: Float = 0f
    var magnetCaptured: Boolean = false
}

class ParticleEntity {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var vz: Float = 0f
    var life: Float = 0f
    var maxLife: Float = 0.5f
    var scale: Float = 0.2f
    var r: Float = 1f
    var g: Float = 0.85f
    var b: Float = 0.1f
}

data class HudSnapshot(
    val stageNumber: Int = 1,
    val biomeName: String = "",
    val distanceMeters: Int = 0,
    val targetMeters: Int = StageGenerator.STAGE_DISTANCE_METERS,
    val remainingMeters: Int = StageGenerator.STAGE_DISTANCE_METERS,
    val score: Long = 0L,
    val coins: Int = 0,
    val diamonds: Int = 0,
    val speedKmh: Int = 65,
    val magnetRemaining: Float = 0f,
    val magnetMax: Float = 8f,
    val jetpackRemaining: Float = 0f,
    val jetpackMax: Float = 6f,
    val sneakersRemaining: Float = 0f,
    val sneakersMax: Float = 10f,
    val score2xRemaining: Float = 0f,
    val score2xMax: Float = 12f,
    val shieldRemaining: Float = 0f,
    val shieldMax: Float = 14f,
    val hoverboardRemaining: Float = 0f,
    val hoverboardMax: Float = 15f,
    val isFinished: Boolean = false,
    val isStageCompleted: Boolean = false,
    val isPaused: Boolean = false
)

interface SimulationEvents {
    fun onSound(effect: SoundEffect)
    fun onHaptic(durationMs: Long, amplitude: Int)
    fun onGameOver(distanceMeters: Int, score: Long, coins: Int, diamonds: Int, jumps: Int, powerUps: Int)
    fun onStageCompleted(distanceMeters: Int, score: Long, coins: Int, diamonds: Int, jumps: Int, powerUps: Int)
}

class GameWorldSimulation(
    private val stageConfig: StageConfig,
    private val powerUpLevels: Map<PowerUpType, Int>,
    private val hoverboardSpeedBonus: Float = 1.08f,
    private val events: SimulationEvents? = null
) {
    companion object {
        const val LANE_WIDTH = 2.4f
        const val SEGMENT_LENGTH = 16f
        const val SEGMENT_COUNT = 12
        const val TRAIN_ROOF_Y = 2.55f
        const val JETPACK_Y = 5.4f
    }

    private val rng = Random(stageConfig.stageNumber * 9973 + 17)

    // Object Pools (zero allocation in gameplay loop)
    val segments = Array(SEGMENT_COUNT) { TrackSegment() }
    val obstacles = Array(42) { ObstacleEntity() }
    val collectibles = Array(140) { CollectibleEntity() }
    val particles = Array(90) { ParticleEntity() }

    // Player physics & animation state
    var targetLane: Int = 0
        private set
    var playerX: Float = 0f
        private set
    var playerY: Float = 0f
        private set
    var playerVy: Float = 0f
        private set
    var isSliding: Boolean = false
        private set
    var slideTimer: Float = 0f
        private set
    var runAnimPhase: Float = 0f
        private set
    var fallTimer: Float = 0f
        private set
    var invulnerableTimer: Float = 0f
        private set
    var cameraShake: Float = 0f
        private set
    var cameraHeight: Float = 4.6f
        private set

    // Progression & stats
    var distanceFloat: Float = 0f
        private set
    var scoreFloat: Double = 0.0
        private set
    var coinsCollected: Int = 0
        private set
    var diamondsCollected: Int = 0
        private set
    var jumpsMade: Int = 0
        private set
    var powerUpsActivated: Int = 0
        private set
    var currentSpeed: Float = stageConfig.baseSpeed
        private set

    var isPaused: Boolean = false
    var isGameOver: Boolean = false
        private set
    var isStageCompleted: Boolean = false
        private set

    // Active PowerUp timers
    val activeTimers = mutableMapOf<PowerUpType, Float>().apply {
        PowerUpType.entries.forEach { put(it, 0f) }
    }

    private var nextSpawnZ = -48f
    private var footstepTimer = 0f
    private var jetpackCoinCooldown = 0f
    private var patternCounter = 0

    init {
        resetWorld()
    }

    fun resetWorld() {
        targetLane = 0
        playerX = 0f
        playerY = 0f
        playerVy = 0f
        isSliding = false
        slideTimer = 0f
        runAnimPhase = 0f
        fallTimer = 0f
        invulnerableTimer = 0f
        cameraShake = 0f
        cameraHeight = 4.6f
        distanceFloat = 0f
        scoreFloat = 0.0
        coinsCollected = 0
        diamondsCollected = 0
        jumpsMade = 0
        powerUpsActivated = 0
        currentSpeed = stageConfig.baseSpeed
        isPaused = false
        isGameOver = false
        isStageCompleted = false
        PowerUpType.entries.forEach { activeTimers[it] = 0f }

        obstacles.forEach { it.active = false }
        collectibles.forEach { it.active = false }
        particles.forEach { it.active = false }

        for (i in segments.indices) {
            segments[i].apply {
                active = true
                zCenter = 8f - i * SEGMENT_LENGTH
                style = SegmentStyle.OPEN_STATION
                hasLeftPlatform = true
                hasRightPlatform = true
                leftBuildingHeight = 9f + rng.nextFloat() * 14f
                rightBuildingHeight = 9f + rng.nextFloat() * 14f
            }
        }
        nextSpawnZ = -44f
        while (nextSpawnZ > -140f) {
            spawnPatternAt(nextSpawnZ)
            nextSpawnZ -= 26f
        }
    }

    fun swipeLeft() {
        if (isPaused || isGameOver || isStageCompleted) return
        if (targetLane > -1) {
            targetLane--
            events?.onSound(SoundEffect.LANE_SWITCH)
        }
    }

    fun swipeRight() {
        if (isPaused || isGameOver || isStageCompleted) return
        if (targetLane < 1) {
            targetLane++
            events?.onSound(SoundEffect.LANE_SWITCH)
        }
    }

    fun swipeUp() {
        if (isPaused || isGameOver || isStageCompleted) return
        if ((activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f) return
        val groundY = currentGroundSupportY()
        if (playerY <= groundY + 0.14f) {
            isSliding = false
            slideTimer = 0f
            val sneakersActive = (activeTimers[PowerUpType.SNEAKERS] ?: 0f) > 0f
            playerVy = if (sneakersActive) 18.8f else 14.4f
            jumpsMade++
            events?.onSound(SoundEffect.JUMP)
            spawnBurst(playerX, playerY + 0.2f, 0f, 0.2f, 0.9f, 1f, 8)
        }
    }

    fun swipeDown() {
        if (isPaused || isGameOver || isStageCompleted) return
        if ((activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f) return
        val groundY = currentGroundSupportY()
        if (playerY > groundY + 0.35f) {
            // Fast slam downward!
            playerVy = min(playerVy, -26f)
        }
        isSliding = true
        slideTimer = 0.72f
        events?.onSound(SoundEffect.SLIDE)
    }

    fun activateHoverboard(): Boolean {
        if (isPaused || isGameOver || isStageCompleted) return false
        if ((activeTimers[PowerUpType.HOVERBOARD] ?: 0f) > 0f) return false
        val level = powerUpLevels[PowerUpType.HOVERBOARD] ?: 1
        activeTimers[PowerUpType.HOVERBOARD] = PowerUpType.HOVERBOARD.durationForLevel(level)
        powerUpsActivated++
        events?.onSound(SoundEffect.POWERUP)
        events?.onHaptic(40L, 160)
        spawnBurst(playerX, playerY + 0.4f, 0f, 0.0f, 0.95f, 1.0f, 14)
        return true
    }

    fun reviveAfterCrash() {
        if (!isGameOver) return
        isGameOver = false
        fallTimer = 0f
        invulnerableTimer = 2.6f
        playerY = 0f
        playerVy = 0f
        isSliding = false
        // Clear obstacles near player
        for (obs in obstacles) {
            if (obs.active && obs.z > -32f && obs.z < 12f) {
                obs.active = false
            }
        }
        spawnBurst(playerX, 1.0f, 0f, 0.2f, 1.0f, 0.9f, 20)
    }

    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        updateParticles(dt)
        if (cameraShake > 0f) cameraShake = (cameraShake - dt * 2.5f).coerceAtLeast(0f)

        if (isGameOver) {
            fallTimer += dt
            return
        }
        if (isPaused || isStageCompleted) return

        // Speed progression within stage
        val stageProgress = (distanceFloat / stageConfig.targetDistanceMeters).coerceIn(0f, 1f)
        val hoverMult = if ((activeTimers[PowerUpType.HOVERBOARD] ?: 0f) > 0f) hoverboardSpeedBonus else 1f
        val jetpackMult = if ((activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f) 1.38f else 1f
        val targetBaseSpeed = stageConfig.baseSpeed + stageProgress * (stageConfig.maxSpeed - stageConfig.baseSpeed)
        currentSpeed = targetBaseSpeed * hoverMult * jetpackMult

        val dz = currentSpeed * dt
        distanceFloat += dz
        val scoreMult = if ((activeTimers[PowerUpType.SCORE_2X] ?: 0f) > 0f) 2.0 else 1.0
        scoreFloat += dz * 12.0 * scoreMult

        // Decrement powerup & invulnerability timers
        if (invulnerableTimer > 0f) invulnerableTimer = (invulnerableTimer - dt).coerceAtLeast(0f)
        for (type in PowerUpType.entries) {
            val rem = activeTimers[type] ?: 0f
            if (rem > 0f) activeTimers[type] = (rem - dt).coerceAtLeast(0f)
        }

        // Smooth lane X interpolation
        val desiredX = targetLane * LANE_WIDTH
        playerX += (desiredX - playerX) * min(1f, dt * 15f)

        // Slide timer
        if (isSliding) {
            slideTimer -= dt
            if (slideTimer <= 0f) isSliding = false
        }

        // Vertical physics
        val jetpackActive = (activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f
        if (jetpackActive) {
            playerY += (JETPACK_Y - playerY) * min(1f, dt * 6.5f)
            playerVy = 0f
            isSliding = false
            jetpackCoinCooldown -= dt
            if (jetpackCoinCooldown <= 0f) {
                jetpackCoinCooldown = 0.22f
                spawnParticle(playerX - 0.22f, playerY + 0.5f, 0.3f, 0f, -2f, 4f, 0.35f, 0.25f, 1f, 0.45f, 0.1f)
                spawnParticle(playerX + 0.22f, playerY + 0.5f, 0.3f, 0f, -2f, 4f, 0.35f, 0.25f, 1f, 0.85f, 0.2f)
            }
        } else {
            val gravity = -42f
            playerVy += gravity * dt
            playerY += playerVy * dt
            val supportY = currentGroundSupportY()
            if (playerY <= supportY) {
                playerY = supportY
                playerVy = 0f
            }
        }

        // Running cycle animation & footsteps
        val onGround = playerY <= currentGroundSupportY() + 0.08f && !jetpackActive
        if (onGround && !isSliding) {
            runAnimPhase += dt * (currentSpeed * 0.52f)
            footstepTimer -= dt
            if (footstepTimer <= 0f) {
                footstepTimer = (0.28f * (18f / currentSpeed)).coerceIn(0.14f, 0.30f)
                events?.onSound(SoundEffect.FOOTSTEP)
            }
        }

        // Smooth camera vertical follow
        val desiredCamY = 4.6f + playerY * 0.72f
        cameraHeight += (desiredCamY - cameraHeight) * min(1f, dt * 7f)

        // Move & recycle track segments
        for (seg in segments) {
            seg.zCenter += dz
            if (seg.zCenter > SEGMENT_LENGTH * 1.5f) {
                var minZ = 0f
                for (other in segments) {
                    if (other.zCenter < minZ) minZ = other.zCenter
                }
                seg.zCenter = minZ - SEGMENT_LENGTH
                val roll = rng.nextFloat()
                seg.style = when {
                    roll < stageConfig.tunnelProbability -> SegmentStyle.TUNNEL
                    roll < stageConfig.tunnelProbability + stageConfig.bridgeProbability -> SegmentStyle.BRIDGE
                    else -> SegmentStyle.OPEN_STATION
                }
                seg.leftBuildingHeight = 8f + rng.nextFloat() * 18f
                seg.rightBuildingHeight = 8f + rng.nextFloat() * 18f
            }
        }

        // Move obstacles & spawn new ones
        nextSpawnZ += dz
        while (nextSpawnZ > -135f) {
            spawnPatternAt(nextSpawnZ)
            val gap = (28f - stageConfig.obstacleDensity * 8f).coerceIn(18f, 29f)
            nextSpawnZ -= gap
        }

        for (obs in obstacles) {
            if (!obs.active) continue
            obs.z += dz + obs.extraSpeed * dt
            if (obs.z > 14f) {
                obs.active = false
                continue
            }
            if (checkCollision(obs)) {
                handleCrash(obs)
                if (isGameOver) return
            }
        }

        // Move & collect collectibles
        val magnetActive = (activeTimers[PowerUpType.MAGNET] ?: 0f) > 0f
        for (item in collectibles) {
            if (!item.active) continue
            item.z += dz
            item.spinPhase += dt * 4.2f
            if (item.z > 10f) {
                item.active = false
                continue
            }
            if (magnetActive && item.kind == CollectibleKind.COIN && item.z > -22f && item.z < 3f) {
                item.magnetCaptured = true
            }
            if (item.magnetCaptured) {
                item.x += (playerX - item.x) * min(1f, dt * 14f)
                item.y += ((playerY + 0.9f) - item.y) * min(1f, dt * 14f)
                item.z += (0f - item.z) * min(1f, dt * 14f)
            }
            val dx = abs(item.x - playerX)
            val playerCenterY = playerY + if (isSliding) 0.45f else 0.95f
            val dy = abs(item.y - playerCenterY)
            val dzItem = abs(item.z)
            val collectRadius = if (item.magnetCaptured) 1.3f else 1.15f
            if (dx < collectRadius && dy < 1.35f && dzItem < 1.3f) {
                collectItem(item)
            }
        }

        // Stage completion check (exact 5000m target)
        if (distanceFloat >= stageConfig.targetDistanceMeters) {
            distanceFloat = stageConfig.targetDistanceMeters.toFloat()
            isStageCompleted = true
            events?.onSound(SoundEffect.STAGE_COMPLETE)
            events?.onHaptic(80L, 220)
            events?.onStageCompleted(
                distanceMeters = stageConfig.targetDistanceMeters,
                score = scoreFloat.toLong(),
                coins = coinsCollected,
                diamonds = diamondsCollected,
                jumps = jumpsMade,
                powerUps = powerUpsActivated
            )
        }
    }

    private fun currentGroundSupportY(): Float {
        // Check if player is directly above any train roof
        for (obs in obstacles) {
            if (!obs.active) continue
            if (obs.kind == ObstacleKind.PARKED_TRAIN || obs.kind == ObstacleKind.MOVING_TRAIN) {
                val halfLen = obs.length * 0.5f + 0.3f
                val laneX = obs.lane * LANE_WIDTH
                if (abs(playerX - laneX) < 1.05f && abs(obs.z) < halfLen && playerY >= TRAIN_ROOF_Y - 0.55f) {
                    return TRAIN_ROOF_Y
                }
            } else if (obs.kind == ObstacleKind.RAMP) {
                val halfLen = obs.length * 0.5f
                val laneX = obs.lane * LANE_WIDTH
                if (abs(playerX - laneX) < 1.05f && obs.z in -halfLen..halfLen) {
                    // Ramp slopes up from +halfLen (y=0) to -halfLen (y=TRAIN_ROOF_Y)
                    val t = ((halfLen - obs.z) / (2f * halfLen)).coerceIn(0f, 1f)
                    return t * TRAIN_ROOF_Y
                }
            }
        }
        return 0f
    }

    private fun checkCollision(obs: ObstacleEntity): Boolean {
        if (obs.hitOnce) return false
        if ((activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f) return false
        if (invulnerableTimer > 0f) return false

        val laneX = obs.lane * LANE_WIDTH
        if (abs(playerX - laneX) > 0.88f) return false

        val halfLen = obs.length * 0.5f
        if (abs(obs.z) > halfLen + 0.35f) return false

        if (obs.kind == ObstacleKind.RAMP) {
            // Ramp helps player climb instead of hurting!
            val t = ((halfLen - obs.z) / (2f * halfLen)).coerceIn(0f, 1f)
            val rampY = t * TRAIN_ROOF_Y
            if (playerY < rampY) {
                playerY = rampY
                playerVy = maxOf(playerVy, 2f)
            }
            return false
        }

        val playerBottom = playerY
        val playerTop = playerY + if (isSliding) 0.72f else 1.72f

        if (obs.kind == ObstacleKind.PARKED_TRAIN || obs.kind == ObstacleKind.MOVING_TRAIN) {
            // Safe if running on top of train roof!
            if (playerBottom >= TRAIN_ROOF_Y - 0.45f) return false
            return true
        }

        val obsBottom = obs.clearanceBottomY
        val obsTop = obsBottom + obs.height
        return playerTop > obsBottom + 0.1f && playerBottom < obsTop - 0.12f
    }

    private fun handleCrash(obs: ObstacleEntity) {
        obs.hitOnce = true
        obs.active = false
        cameraShake = 1.0f

        val shieldRem = activeTimers[PowerUpType.SHIELD] ?: 0f
        if (shieldRem > 0f) {
            activeTimers[PowerUpType.SHIELD] = 0f
            invulnerableTimer = 1.8f
            events?.onSound(SoundEffect.SHIELD_BREAK)
            events?.onHaptic(60L, 200)
            spawnBurst(playerX, playerY + 1.0f, 0f, 0.2f, 0.9f, 1.0f, 18)
            return
        }

        val hoverRem = activeTimers[PowerUpType.HOVERBOARD] ?: 0f
        if (hoverRem > 0f) {
            activeTimers[PowerUpType.HOVERBOARD] = 0f
            invulnerableTimer = 2.0f
            events?.onSound(SoundEffect.SHIELD_BREAK)
            events?.onHaptic(65L, 210)
            spawnBurst(playerX, playerY + 0.5f, 0f, 1.0f, 0.6f, 0.15f, 20)
            return
        }

        // Lethal wipeout
        isGameOver = true
        fallTimer = 0f
        events?.onSound(SoundEffect.COLLISION)
        events?.onSound(SoundEffect.GAME_OVER)
        events?.onHaptic(110L, 255)
        spawnBurst(playerX, playerY + 0.9f, 0f, 1.0f, 0.3f, 0.3f, 24)
        events?.onGameOver(
            distanceMeters = distanceFloat.toInt(),
            score = scoreFloat.toLong(),
            coins = coinsCollected,
            diamonds = diamondsCollected,
            jumps = jumpsMade,
            powerUps = powerUpsActivated
        )
    }

    private fun collectItem(item: CollectibleEntity) {
        item.active = false
        when (item.kind) {
            CollectibleKind.COIN -> {
                val mult = if ((activeTimers[PowerUpType.SCORE_2X] ?: 0f) > 0f) 2 else 1
                coinsCollected += 1
                scoreFloat += 25.0 * mult
                events?.onSound(SoundEffect.COIN)
                spawnBurst(item.x, item.y, item.z, 1.0f, 0.84f, 0.10f, 6)
            }
            CollectibleKind.DIAMOND -> {
                val mult = if ((activeTimers[PowerUpType.SCORE_2X] ?: 0f) > 0f) 2 else 1
                diamondsCollected += 1
                scoreFloat += 250.0 * mult
                events?.onSound(SoundEffect.DIAMOND)
                events?.onHaptic(30L, 140)
                spawnBurst(item.x, item.y, item.z, 0.25f, 0.92f, 1.0f, 14)
            }
            CollectibleKind.POWERUP -> {
                val type = item.powerUpType
                val lv = powerUpLevels[type] ?: 1
                activeTimers[type] = type.durationForLevel(lv)
                powerUpsActivated++
                events?.onSound(SoundEffect.POWERUP)
                events?.onHaptic(40L, 170)
                if (type == PowerUpType.JETPACK) {
                    spawnSkyCoinsTrail(item.lane)
                }
                spawnBurst(item.x, item.y, item.z, 0.85f, 0.35f, 1.0f, 14)
            }
        }
    }

    private fun spawnSkyCoinsTrail(startLane: Int) {
        var lane = startLane
        for (i in 0 until 18) {
            if (i % 5 == 0) lane = rng.nextInt(-1, 2)
            spawnCollectible(CollectibleKind.COIN, lane, JETPACK_Y + 0.6f, -14f - i * 4.2f)
        }
    }

    private fun spawnPatternAt(zBase: Float) {
        patternCounter++
        val lanes = listOf(-1, 0, 1).shuffled(rng)
        val openLane = lanes[0]
        val secondLane = lanes[1]
        val thirdLane = lanes[2]

        when (patternCounter % 5) {
            0 -> {
                // Moving or Parked Train on 1-2 lanes, always leaving openLane clear!
                val isMoving = rng.nextFloat() < stageConfig.movingTrainRatio
                spawnObstacle(
                    kind = if (isMoving) ObstacleKind.MOVING_TRAIN else ObstacleKind.PARKED_TRAIN,
                    lane = secondLane,
                    z = zBase - 6f,
                    length = 12f,
                    height = TRAIN_ROOF_Y,
                    extraSpeed = if (isMoving) 13f else 0f
                )
                if (rng.nextFloat() < stageConfig.obstacleDensity * 0.65f) {
                    // Ramp leading onto parked train in third lane
                    spawnObstacle(ObstacleKind.RAMP, thirdLane, zBase + 3.5f, length = 5.2f, height = TRAIN_ROOF_Y)
                    spawnObstacle(ObstacleKind.PARKED_TRAIN, thirdLane, zBase - 5.5f, length = 12.5f, height = TRAIN_ROOF_Y)
                    spawnCoinArc(thirdLane, zBase - 2f, count = 5, baseY = TRAIN_ROOF_Y + 0.85f)
                }
                spawnCoinArc(openLane, zBase, count = 5, baseY = 0.85f)
            }
            1 -> {
                // Low hurdle + High slide gate combo
                spawnObstacle(ObstacleKind.LOW_BARRIER, openLane, zBase, length = 1.2f, height = 1.05f)
                spawnCoinArc(openLane, zBase, count = 3, baseY = 2.2f)
                spawnObstacle(ObstacleKind.HIGH_GATE, secondLane, zBase - 2f, length = 1.2f, height = 1.7f, clearanceBottomY = 1.22f)
                spawnCoinArc(secondLane, zBase - 2f, count = 3, baseY = 0.65f)
                if (rng.nextFloat() < stageConfig.obstacleDensity) {
                    spawnObstacle(ObstacleKind.FULL_BARRIER, thirdLane, zBase - 1f, length = 1.4f, height = 2.6f)
                }
            }
            2 -> {
                // Two trains side by side + clear center/side dash with diamond or powerup!
                spawnObstacle(ObstacleKind.PARKED_TRAIN, secondLane, zBase - 5f, length = 12f, height = TRAIN_ROOF_Y)
                if (rng.nextFloat() < stageConfig.obstacleDensity * 0.75f) {
                    val mv = rng.nextFloat() < stageConfig.movingTrainRatio
                    spawnObstacle(
                        kind = if (mv) ObstacleKind.MOVING_TRAIN else ObstacleKind.LOW_BARRIER,
                        lane = thirdLane,
                        z = zBase - 5f,
                        length = if (mv) 11f else 1.2f,
                        height = if (mv) TRAIN_ROOF_Y else 1.05f,
                        extraSpeed = if (mv) 12f else 0f
                    )
                }
                spawnCoinArc(openLane, zBase - 2f, count = 5, baseY = 0.85f)
            }
            3 -> {
                // High gate + Full barrier + diamond reward
                spawnObstacle(ObstacleKind.HIGH_GATE, openLane, zBase, length = 1.2f, height = 1.7f, clearanceBottomY = 1.22f)
                spawnObstacle(ObstacleKind.FULL_BARRIER, secondLane, zBase, length = 1.4f, height = 2.6f)
                spawnObstacle(ObstacleKind.LOW_BARRIER, thirdLane, zBase - 3f, length = 1.2f, height = 1.05f)
                spawnCoinArc(openLane, zBase, count = 4, baseY = 0.65f)
            }
            else -> {
                // Express dual-train rush + ramp to rooftop coins
                spawnObstacle(ObstacleKind.RAMP, openLane, zBase + 3.8f, length = 5.0f, height = TRAIN_ROOF_Y)
                spawnObstacle(ObstacleKind.PARKED_TRAIN, openLane, zBase - 5.2f, length = 12.5f, height = TRAIN_ROOF_Y)
                spawnCoinArc(openLane, zBase - 3f, count = 5, baseY = TRAIN_ROOF_Y + 0.85f)
                spawnObstacle(ObstacleKind.MOVING_TRAIN, secondLane, zBase - 7f, length = 11.5f, height = TRAIN_ROOF_Y, extraSpeed = 14f)
                spawnCoinArc(thirdLane, zBase, count = 4, baseY = 0.85f)
            }
        }

        // Occasional rare Diamond or Power-Up spawn
        if (patternCounter % 7 == 0) {
            spawnCollectible(CollectibleKind.DIAMOND, openLane, 1.35f, zBase - 11f)
        } else if (patternCounter % 6 == 0) {
            val pickupTypes = listOf(
                PowerUpType.MAGNET,
                PowerUpType.JETPACK,
                PowerUpType.SNEAKERS,
                PowerUpType.SCORE_2X,
                PowerUpType.SHIELD
            )
            val chosen = pickupTypes[rng.nextInt(pickupTypes.size)]
            spawnCollectible(CollectibleKind.POWERUP, openLane, 1.25f, zBase - 10f, chosen)
        }
    }

    private fun spawnCoinArc(lane: Int, zCenter: Float, count: Int, baseY: Float) {
        for (i in 0 until count) {
            val z = zCenter - i * 2.5f
            val arcBump = if (baseY > 1.5f && i == count / 2) 0.55f else 0f
            spawnCollectible(CollectibleKind.COIN, lane, baseY + arcBump, z)
        }
    }

    private fun spawnObstacle(
        kind: ObstacleKind,
        lane: Int,
        z: Float,
        length: Float,
        height: Float,
        clearanceBottomY: Float = 0f,
        extraSpeed: Float = 0f
    ) {
        val slot = obstacles.firstOrNull { !it.active } ?: return
        slot.active = true
        slot.kind = kind
        slot.lane = lane
        slot.z = z
        slot.length = length
        slot.width = 1.8f
        slot.height = height
        slot.clearanceBottomY = clearanceBottomY
        slot.extraSpeed = extraSpeed
        slot.hitOnce = false
    }

    private fun spawnCollectible(
        kind: CollectibleKind,
        lane: Int,
        y: Float,
        z: Float,
        powerUpType: PowerUpType = PowerUpType.MAGNET
    ) {
        val slot = collectibles.firstOrNull { !it.active } ?: return
        slot.active = true
        slot.kind = kind
        slot.powerUpType = powerUpType
        slot.lane = lane
        slot.x = lane * LANE_WIDTH
        slot.y = y
        slot.z = z
        slot.spinPhase = rng.nextFloat() * 6.28f
        slot.magnetCaptured = false
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int) {
        for (i in 0 until count) {
            spawnParticle(
                x, y, z,
                (rng.nextFloat() - 0.5f) * 6f,
                rng.nextFloat() * 5f + 1.5f,
                (rng.nextFloat() - 0.5f) * 6f,
                0.45f,
                0.18f,
                r, g, b
            )
        }
    }

    private fun spawnParticle(
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        life: Float, scale: Float,
        r: Float, g: Float, b: Float
    ) {
        val p = particles.firstOrNull { !it.active } ?: return
        p.active = true
        p.x = x; p.y = y; p.z = z
        p.vx = vx; p.vy = vy; p.vz = vz
        p.life = life; p.maxLife = life
        p.scale = scale
        p.r = r; p.g = g; p.b = b
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.vy -= 12f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
        }
    }

    fun snapshot(): HudSnapshot {
        val dist = distanceFloat.toInt().coerceIn(0, stageConfig.targetDistanceMeters)
        val rem = (stageConfig.targetDistanceMeters - dist).coerceAtLeast(0)
        return HudSnapshot(
            stageNumber = stageConfig.stageNumber,
            biomeName = stageConfig.biome.title,
            distanceMeters = dist,
            targetMeters = stageConfig.targetDistanceMeters,
            remainingMeters = rem,
            score = scoreFloat.toLong(),
            coins = coinsCollected,
            diamonds = diamondsCollected,
            speedKmh = (currentSpeed * 3.6f).toInt(),
            magnetRemaining = activeTimers[PowerUpType.MAGNET] ?: 0f,
            magnetMax = PowerUpType.MAGNET.durationForLevel(powerUpLevels[PowerUpType.MAGNET] ?: 1),
            jetpackRemaining = activeTimers[PowerUpType.JETPACK] ?: 0f,
            jetpackMax = PowerUpType.JETPACK.durationForLevel(powerUpLevels[PowerUpType.JETPACK] ?: 1),
            sneakersRemaining = activeTimers[PowerUpType.SNEAKERS] ?: 0f,
            sneakersMax = PowerUpType.SNEAKERS.durationForLevel(powerUpLevels[PowerUpType.SNEAKERS] ?: 1),
            score2xRemaining = activeTimers[PowerUpType.SCORE_2X] ?: 0f,
            score2xMax = PowerUpType.SCORE_2X.durationForLevel(powerUpLevels[PowerUpType.SCORE_2X] ?: 1),
            shieldRemaining = activeTimers[PowerUpType.SHIELD] ?: 0f,
            shieldMax = PowerUpType.SHIELD.durationForLevel(powerUpLevels[PowerUpType.SHIELD] ?: 1),
            hoverboardRemaining = activeTimers[PowerUpType.HOVERBOARD] ?: 0f,
            hoverboardMax = PowerUpType.HOVERBOARD.durationForLevel(powerUpLevels[PowerUpType.HOVERBOARD] ?: 1),
            isFinished = isGameOver || isStageCompleted,
            isStageCompleted = isStageCompleted,
            isPaused = isPaused
        )
    }
}
