package com.example.game3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.CharacterDef
import com.example.data.GraphicsQuality
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import com.example.data.PowerUpType
import com.example.data.StageConfig
import kotlin.math.sin
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Metro3DRenderer(
    private val simulation: GameWorldSimulation,
    private val stageConfig: StageConfig,
    private val graphicsQuality: GraphicsQuality,
    private val targetFps: Int,
    private val character: CharacterDef,
    private val outfit: OutfitDef?,
    private val hoverboard: HoverboardDef,
    private val onHudTick: (HudSnapshot) -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var meshes: MeshCatalog
    private val characterRenderer = CharacterModelRenderer()

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNanos = System.nanoTime()
    private var hudAccumulator = 0f
    private var globalTime = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val biome = stageConfig.biome
        GLES20.glClearColor(biome.skyTop[0], biome.skyTop[1], biome.skyTop[2], 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        shader = ShaderProgram()
        meshes = MeshCatalog()
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projMatrix, 0, 56f, ratio, 0.5f, 210f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNanos = now
        globalTime += dt

        simulation.step(dt)
        hudAccumulator += dt
        if (hudAccumulator >= 0.05f || simulation.isGameOver || simulation.isStageCompleted) {
            hudAccumulator = 0f
            onHudTick(simulation.snapshot())
        }

        // Frame pacing for 30 FPS mode if selected
        if (targetFps <= 30) {
            val frameMs = (System.nanoTime() - now) / 1_000_000L
            if (frameMs < 30L) {
                try {
                    Thread.sleep(30L - frameMs)
                } catch (_: InterruptedException) {
                }
            }
        }

        val biome = stageConfig.biome
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        shader.use()
        GLES20.glUniform3f(shader.uFogColor, biome.fogColor[0], biome.fogColor[1], biome.fogColor[2])
        val fogDensity = when (graphicsQuality) {
            GraphicsQuality.LOW -> 0.011f
            GraphicsQuality.MEDIUM -> 0.0095f
            GraphicsQuality.HIGH -> 0.0085f
        }
        GLES20.glUniform1f(shader.uFogDensity, fogDensity)
        GLES20.glUniform3f(shader.uLightDir, 0.45f, 0.85f, 0.35f)

        val shakeX = if (simulation.cameraShake > 0f) sin(globalTime * 48f) * simulation.cameraShake * 0.25f else 0f
        val shakeY = if (simulation.cameraShake > 0f) sin(globalTime * 36f) * simulation.cameraShake * 0.20f else 0f
        val camX = simulation.playerX * 0.55f + shakeX
        val camY = simulation.cameraHeight + shakeY
        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, 7.6f,
            simulation.playerX * 0.38f, simulation.playerY + 1.5f, -18f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        drawHorizonGlow()
        drawTrackSegments()
        drawObstacles()
        drawCollectibles()
        drawRunner()
        drawParticles()
    }

    private fun drawHorizonGlow() {
        val biome = stageConfig.biome
        // Distant glowing horizon band
        drawBox(
            0f, 14f, -155f,
            0f, 0f, 0f,
            190f, 34f, 2f,
            biome.skyHorizon[0], biome.skyHorizon[1], biome.skyHorizon[2], 1f, 0.72f
        )
    }

    private fun drawTrackSegments() {
        val biome = stageConfig.biome
        val detailHigh = graphicsQuality != GraphicsQuality.LOW
        for (seg in simulation.segments) {
            if (!seg.active) continue
            val z = seg.zCenter

            // Main railway bed
            val bedColor = if (seg.style == SegmentStyle.BRIDGE) {
                floatArrayOf(biome.groundColor[0] * 1.15f, biome.groundColor[1] * 1.15f, biome.groundColor[2] * 1.25f)
            } else biome.groundColor

            drawBox(
                0f, -0.25f, z,
                0f, 0f, 0f,
                8.8f, 0.5f, GameWorldSimulation.SEGMENT_LENGTH,
                bedColor[0], bedColor[1], bedColor[2], 1f, 0f
            )

            // 3 lanes: twin steel rails + glowing center neon strip
            for (lane in -1..1) {
                val lx = lane * GameWorldSimulation.LANE_WIDTH
                drawBox(lx - 0.55f, 0.04f, z, 0f, 0f, 0f, 0.12f, 0.08f, GameWorldSimulation.SEGMENT_LENGTH, 0.72f, 0.76f, 0.84f, 1f, 0.12f)
                drawBox(lx + 0.55f, 0.04f, z, 0f, 0f, 0f, 0.12f, 0.08f, GameWorldSimulation.SEGMENT_LENGTH, 0.72f, 0.76f, 0.84f, 1f, 0.12f)
                if (detailHigh) {
                    drawBox(
                        lx, 0.015f, z,
                        0f, 0f, 0f,
                        0.18f, 0.03f, GameWorldSimulation.SEGMENT_LENGTH,
                        biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.45f
                    )
                }
            }

            // Wooden/concrete railway sleepers
            val sleeperStep = if (graphicsQuality == GraphicsQuality.HIGH) 2f else 4f
            var sz = z - GameWorldSimulation.SEGMENT_LENGTH * 0.5f + 1f
            while (sz < z + GameWorldSimulation.SEGMENT_LENGTH * 0.5f) {
                drawBox(
                    0f, 0.01f, sz,
                    0f, 0f, 0f,
                    7.2f, 0.05f, 0.36f,
                    biome.sleeperColor[0], biome.sleeperColor[1], biome.sleeperColor[2], 1f, 0f
                )
                sz += sleeperStep
            }

            when (seg.style) {
                SegmentStyle.OPEN_STATION -> {
                    // Station Platforms + Skyline Buildings
                    drawBox(-6.0f, 0.55f, z, 0f, 0f, 0f, 3.0f, 1.1f, GameWorldSimulation.SEGMENT_LENGTH, 0.20f, 0.24f, 0.34f, 1f, 0.02f)
                    drawBox(6.0f, 0.55f, z, 0f, 0f, 0f, 3.0f, 1.1f, GameWorldSimulation.SEGMENT_LENGTH, 0.20f, 0.24f, 0.34f, 1f, 0.02f)
                    // Platform neon edge
                    drawBox(-4.52f, 1.08f, z, 0f, 0f, 0f, 0.12f, 0.08f, GameWorldSimulation.SEGMENT_LENGTH, biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.55f)
                    drawBox(4.52f, 1.08f, z, 0f, 0f, 0f, 0.12f, 0.08f, GameWorldSimulation.SEGMENT_LENGTH, biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.55f)

                    // Buildings on left & right
                    val bhL = seg.leftBuildingHeight
                    val bhR = seg.rightBuildingHeight
                    drawBox(-12.5f, bhL * 0.5f - 1f, z, 0f, 0f, 0f, 7.5f, bhL, 12f, biome.buildingColor[0], biome.buildingColor[1], biome.buildingColor[2], 1f, 0.04f)
                    drawBox(12.5f, bhR * 0.5f - 1f, z, 0f, 0f, 0f, 7.5f, bhR, 12f, biome.buildingColor[0] * 1.1f, biome.buildingColor[1] * 1.1f, biome.buildingColor[2] * 1.1f, 1f, 0.04f)

                    if (detailHigh) {
                        // Building accent strip
                        drawBox(-8.7f, bhL * 0.65f, z, 0f, 0f, 0f, 0.2f, bhL * 0.45f, 1.4f, biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.55f)
                        drawBox(8.7f, bhR * 0.60f, z, 0f, 0f, 0f, 0.2f, bhR * 0.45f, 1.4f, biome.trainAccent[0], biome.trainAccent[1], biome.trainAccent[2], 1f, 0.55f)
                    }
                }
                SegmentStyle.BRIDGE -> {
                    // Bridge guardrails + water surface below
                    drawBox(-4.6f, 0.65f, z, 0f, 0f, 0f, 0.25f, 1.3f, GameWorldSimulation.SEGMENT_LENGTH, 0.48f, 0.54f, 0.65f, 1f, 0.1f)
                    drawBox(4.6f, 0.65f, z, 0f, 0f, 0f, 0.25f, 1.3f, GameWorldSimulation.SEGMENT_LENGTH, 0.48f, 0.54f, 0.65f, 1f, 0.1f)
                    // Overhead steel arch truss
                    drawBox(-4.6f, 3.2f, z, 0f, 0f, 0f, 0.35f, 4.8f, 0.6f, 0.45f, 0.52f, 0.62f, 1f, 0.08f)
                    drawBox(4.6f, 3.2f, z, 0f, 0f, 0f, 0.35f, 4.8f, 0.6f, 0.45f, 0.52f, 0.62f, 1f, 0.08f)
                    drawBox(0f, 5.5f, z, 0f, 0f, 0f, 9.6f, 0.45f, 0.6f, biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.45f)
                    // Shimmering water below bridge
                    drawBox(0f, -4.5f, z, 0f, 0f, 0f, 54f, 0.2f, GameWorldSimulation.SEGMENT_LENGTH, 0.06f, 0.24f, 0.42f, 1f, 0.2f)
                }
                SegmentStyle.TUNNEL -> {
                    // Enclosed subway tunnel walls & roof with lit ceiling strips
                    drawBox(-4.8f, 3.0f, z, 0f, 0f, 0f, 0.8f, 6.2f, GameWorldSimulation.SEGMENT_LENGTH, 0.14f, 0.16f, 0.24f, 1f, 0.03f)
                    drawBox(4.8f, 3.0f, z, 0f, 0f, 0f, 0.8f, 6.2f, GameWorldSimulation.SEGMENT_LENGTH, 0.14f, 0.16f, 0.24f, 1f, 0.03f)
                    drawBox(0f, 6.2f, z, 0f, 0f, 0f, 10.4f, 0.7f, GameWorldSimulation.SEGMENT_LENGTH, 0.11f, 0.13f, 0.20f, 1f, 0.02f)
                    // Tunnel glowing ceiling ribs
                    drawBox(0f, 5.75f, z, 0f, 0f, 0f, 5.2f, 0.16f, 0.45f, biome.railGlow[0], biome.railGlow[1], biome.railGlow[2], 1f, 0.8f)
                }
            }
        }
    }

    private fun drawObstacles() {
        val biome = stageConfig.biome
        for (obs in simulation.obstacles) {
            if (!obs.active) continue
            val x = obs.lane * GameWorldSimulation.LANE_WIDTH
            val z = obs.z
            when (obs.kind) {
                ObstacleKind.LOW_BARRIER -> {
                    // Red/gold striped hurdle barrier
                    drawBox(x, 0.52f, z, 0f, 0f, 0f, 1.9f, 0.96f, 0.38f, 0.92f, 0.22f, 0.26f, 1f, 0.15f)
                    drawBox(x, 0.78f, z + 0.20f, 0f, 0f, 0f, 1.75f, 0.24f, 0.06f, 1.0f, 0.82f, 0.12f, 1f, 0.45f)
                    drawBox(x - 0.75f, 0.48f, z, 0f, 0f, 0f, 0.22f, 0.96f, 0.55f, 0.25f, 0.28f, 0.36f, 1f, 0f)
                    drawBox(x + 0.75f, 0.48f, z, 0f, 0f, 0f, 0.22f, 0.96f, 0.55f, 0.25f, 0.28f, 0.36f, 1f, 0f)
                }
                ObstacleKind.HIGH_GATE -> {
                    // Overhead signal gate (slide under!)
                    drawBox(x - 0.88f, 1.55f, z, 0f, 0f, 0f, 0.22f, 3.1f, 0.32f, 0.35f, 0.40f, 0.52f, 1f, 0.05f)
                    drawBox(x + 0.88f, 1.55f, z, 0f, 0f, 0f, 0.22f, 3.1f, 0.32f, 0.35f, 0.40f, 0.52f, 1f, 0.05f)
                    drawBox(x, 2.15f, z, 0f, 0f, 0f, 1.98f, 1.75f, 0.36f, 0.18f, 0.24f, 0.40f, 1f, 0.1f)
                    // Glowing cyan "SLIDE" arrow bar at bottom edge
                    drawBox(x, 1.32f, z + 0.19f, 0f, 0f, 0f, 1.85f, 0.20f, 0.08f, 0.0f, 0.92f, 1.0f, 1f, 0.7f)
                }
                ObstacleKind.FULL_BARRIER -> {
                    drawBox(x, 1.3f, z, 0f, 0f, 0f, 1.95f, 2.6f, 0.85f, 0.82f, 0.28f, 0.14f, 1f, 0.12f)
                    drawBox(x, 1.5f, z + 0.44f, 0f, 0f, 0f, 1.7f, 0.4f, 0.08f, 1.0f, 0.84f, 0.12f, 1f, 0.45f)
                }
                ObstacleKind.RAMP -> {
                    // Glowing sloped ramp up onto parked train
                    drawMesh(
                        meshes.ramp,
                        x, GameWorldSimulation.TRAIN_ROOF_Y * 0.5f, z,
                        0f, 0f, 0f,
                        1.85f, GameWorldSimulation.TRAIN_ROOF_Y, obs.length,
                        biome.trainAccent[0], biome.trainAccent[1], biome.trainAccent[2], 1f, 0.30f
                    )
                }
                ObstacleKind.PARKED_TRAIN, ObstacleKind.MOVING_TRAIN -> {
                    val isMoving = obs.kind == ObstacleKind.MOVING_TRAIN
                    val body = if (isMoving) biome.trainAccent else biome.trainBody
                    val trim = if (isMoving) biome.trainBody else biome.trainAccent
                    val len = obs.length
                    // Main carriage body
                    drawBox(x, 1.32f, z - 0.6f, 0f, 0f, 0f, 1.96f, 2.25f, len - 1.4f, body[0], body[1], body[2], 1f, 0.08f)
                    // Sloped locomotive nose facing player (+Z)
                    drawMesh(
                        meshes.trainNose,
                        x, 1.32f, z + len * 0.5f - 0.65f,
                        0f, 0f, 0f,
                        1.96f, 2.25f, 1.5f,
                        body[0] * 1.05f, body[1] * 1.05f, body[2] * 1.05f, 1f, 0.10f
                    )
                    // Windshield
                    drawBox(x, 1.65f, z + len * 0.5f - 0.25f, -20f, 0f, 0f, 1.48f, 0.65f, 0.25f, 0.06f, 0.12f, 0.22f, 1f, 0.18f)
                    // Dual headlights (brighter if moving train!)
                    val headGlow = if (isMoving) 1.0f else 0.45f
                    drawBox(x - 0.58f, 0.72f, z + len * 0.5f + 0.08f, 0f, 0f, 0f, 0.34f, 0.22f, 0.12f, 1f, 0.96f, 0.72f, 1f, headGlow)
                    drawBox(x + 0.58f, 0.72f, z + len * 0.5f + 0.08f, 0f, 0f, 0f, 0.34f, 0.22f, 0.12f, 1f, 0.96f, 0.72f, 1f, headGlow)
                    // Trim stripe along side & roof AC unit
                    drawBox(x, 1.25f, z - 0.5f, 0f, 0f, 0f, 2.02f, 0.24f, len - 1.6f, trim[0], trim[1], trim[2], 1f, 0.35f)
                    drawBox(x, 2.52f, z - 1.2f, 0f, 0f, 0f, 1.2f, 0.16f, len * 0.45f, 0.22f, 0.26f, 0.34f, 1f, 0.04f)
                    // Bogie undercarriage
                    drawBox(x, 0.22f, z, 0f, 0f, 0f, 1.6f, 0.42f, len * 0.75f, 0.12f, 0.14f, 0.18f, 1f, 0f)
                }
            }
        }
    }

    private fun drawCollectibles() {
        for (item in simulation.collectibles) {
            if (!item.active) continue
            val bob = sin(item.spinPhase * 1.4f) * 0.12f
            val deg = Math.toDegrees(item.spinPhase.toDouble()).toFloat()

            // Ground shadow beneath collectible
            if (graphicsQuality != GraphicsQuality.LOW) {
                drawMesh(meshes.coinDisc, item.x, 0.04f, item.z, 90f, 0f, 0f, 0.55f, 0.55f, 0.05f, 0.02f, 0.03f, 0.06f, 0.35f, 0f)
            }

            when (item.kind) {
                CollectibleKind.COIN -> {
                    drawMesh(
                        meshes.coinDisc,
                        item.x, item.y + bob, item.z,
                        0f, deg, 0f,
                        0.82f, 0.82f, 0.82f,
                        1.0f, 0.82f, 0.10f, 1f, 0.55f
                    )
                }
                CollectibleKind.DIAMOND -> {
                    drawMesh(
                        meshes.diamond,
                        item.x, item.y + bob, item.z,
                        0f, deg * 1.3f, 12f,
                        1.15f, 1.15f, 1.15f,
                        0.25f, 0.92f, 1.0f, 1f, 0.85f
                    )
                }
                CollectibleKind.POWERUP -> {
                    val (r, g, b) = when (item.powerUpType) {
                        PowerUpType.MAGNET -> Triple(1.0f, 0.28f, 0.38f)
                        PowerUpType.JETPACK -> Triple(1.0f, 0.62f, 0.12f)
                        PowerUpType.SNEAKERS -> Triple(0.18f, 0.96f, 0.52f)
                        PowerUpType.SCORE_2X -> Triple(0.84f, 0.32f, 1.0f)
                        PowerUpType.SHIELD -> Triple(0.10f, 0.88f, 1.0f)
                        PowerUpType.HOVERBOARD -> Triple(1.0f, 0.86f, 0.15f)
                    }
                    // Outer energy cube + inner core diamond
                    drawBox(item.x, item.y + bob, item.z, 25f, deg, 25f, 0.72f, 0.72f, 0.72f, r, g, b, 1f, 0.75f)
                }
            }
        }
    }

    private fun drawRunner() {
        val blinkHidden = simulation.invulnerableTimer > 0f && ((simulation.invulnerableTimer * 12f).toInt() % 2 == 0)
        if (blinkHidden) return

        val jetpack = (simulation.activeTimers[PowerUpType.JETPACK] ?: 0f) > 0f
        val sneakers = (simulation.activeTimers[PowerUpType.SNEAKERS] ?: 0f) > 0f
        val hoverboardActive = (simulation.activeTimers[PowerUpType.HOVERBOARD] ?: 0f) > 0f
        val shield = (simulation.activeTimers[PowerUpType.SHIELD] ?: 0f) > 0f

        characterRenderer.drawRunner(
            shader = shader,
            meshes = meshes,
            vpMatrix = vpMatrix,
            x = simulation.playerX,
            y = simulation.playerY,
            z = 0f,
            yawDeg = 0f,
            runPhase = simulation.runAnimPhase,
            isJumping = simulation.playerY > 0.15f && !jetpack,
            isSliding = simulation.isSliding,
            isFalling = simulation.isGameOver,
            fallTimer = simulation.fallTimer,
            jetpackActive = jetpack,
            sneakersActive = sneakers,
            hoverboardActive = hoverboardActive,
            shieldActive = shield,
            character = character,
            outfit = outfit,
            hoverboard = hoverboard,
            drawShadow = graphicsQuality != GraphicsQuality.LOW
        )
    }

    private fun drawParticles() {
        for (p in simulation.particles) {
            if (!p.active) continue
            val s = p.scale * (p.life / p.maxLife).coerceIn(0.1f, 1f)
            drawBox(p.x, p.y, p.z, p.life * 320f, p.life * 210f, 0f, s, s, s, p.r, p.g, p.b, 1f, 0.8f)
        }
    }

    private fun drawBox(
        x: Float, y: Float, z: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        emissive: Float
    ) {
        drawMesh(meshes.cube, x, y, z, rx, ry, rz, sx, sy, sz, r, g, b, a, emissive)
    }

    private fun drawMesh(
        mesh: Mesh,
        x: Float, y: Float, z: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        emissive: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMVPMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(shader.uColor, r, g, b, a)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        mesh.bindAndDraw(shader.aPosition, shader.aNormal)
    }
}
