package com.example.game3d

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.data.CharacterDef
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CharacterModelRenderer {
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    fun drawRunner(
        shader: ShaderProgram,
        meshes: MeshCatalog,
        vpMatrix: FloatArray,
        x: Float,
        y: Float,
        z: Float,
        yawDeg: Float,
        runPhase: Float,
        isJumping: Boolean,
        isSliding: Boolean,
        isFalling: Boolean,
        fallTimer: Float,
        jetpackActive: Boolean,
        sneakersActive: Boolean,
        hoverboardActive: Boolean,
        shieldActive: Boolean,
        character: CharacterDef,
        outfit: OutfitDef?,
        hoverboard: HoverboardDef,
        drawShadow: Boolean
    ) {
        val primary = outfit?.primaryColor ?: character.primaryColor
        val secondary = outfit?.secondaryColor ?: character.secondaryColor
        val accent = outfit?.accentColor ?: character.visorColor

        if (drawShadow && y >= 0f) {
            val shadowScale = (1f - min(y, 6f) * 0.1f).coerceIn(0.35f, 1f)
            drawPart(
                shader, meshes.coinDisc, vpMatrix,
                x, 0.03f, z,
                90f, 0f, 0f,
                1.05f * shadowScale, 1.05f * shadowScale, 0.05f,
                0.02f, 0.03f, 0.06f, 0.45f, 0f
            )
        }

        // Compute root pose
        val rootTiltX = when {
            isFalling -> min(95f, fallTimer * 260f)
            isSliding -> -65f
            jetpackActive -> 22f
            isJumping -> 10f
            else -> 8f
        }
        val rootYOffset = when {
            isFalling -> -0.25f * min(1f, fallTimer * 3f)
            isSliding -> 0.42f
            hoverboardActive -> 0.25f + sin(runPhase * 1.4f) * 0.05f
            else -> absBob(runPhase, isJumping || jetpackActive)
        }

        val swing = if (isJumping || isSliding || isFalling || hoverboardActive || jetpackActive) {
            0f
        } else {
            sin(runPhase) * 38f
        }
        val armSwing = if (jetpackActive) -20f else -swing * 1.05f
        val legSwing = when {
            isJumping -> 28f
            hoverboardActive -> 12f
            else -> swing
        }

        // Hoverboard beneath feet
        if (hoverboardActive) {
            drawPart(
                shader, meshes.cube, vpMatrix,
                x, y + 0.14f, z,
                rootTiltX * 0.2f, yawDeg, 0f,
                0.78f, 0.10f, 1.85f,
                hoverboard.deckColor[0], hoverboard.deckColor[1], hoverboard.deckColor[2], 1f, 0.18f
            )
            drawPart(
                shader, meshes.cube, vpMatrix,
                x, y + 0.11f, z + 0.65f,
                0f, yawDeg, 0f,
                0.55f, 0.07f, 0.35f,
                hoverboard.glowColor[0], hoverboard.glowColor[1], hoverboard.glowColor[2], 1f, 0.65f
            )
        }

        val baseY = y + rootYOffset
        val radYaw = Math.toRadians(yawDeg.toDouble())
        val sinY = sin(radYaw).toFloat()
        val cosY = cos(radYaw).toFloat()

        fun localOffset(lx: Float, ly: Float, lz: Float): Triple<Float, Float, Float> {
            val wx = x + lx * cosY + lz * sinY
            val wz = z - lx * sinY + lz * cosY
            return Triple(wx, baseY + ly, wz)
        }

        // Torso
        val torsoPos = localOffset(0f, 1.05f, 0f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            torsoPos.first, torsoPos.second, torsoPos.third,
            rootTiltX, yawDeg, 0f,
            0.62f, 0.72f, 0.38f,
            primary[0], primary[1], primary[2], 1f, 0.06f
        )

        // Chest accent stripe
        val stripePos = localOffset(0f, 1.08f, -0.20f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            stripePos.first, stripePos.second, stripePos.third,
            rootTiltX, yawDeg, 0f,
            0.40f, 0.20f, 0.04f,
            accent[0], accent[1], accent[2], 1f, 0.35f
        )

        // Backpack or Jetpack
        val packPos = localOffset(0f, 1.08f, 0.25f)
        if (jetpackActive) {
            drawPart(
                shader, meshes.cube, vpMatrix,
                packPos.first - 0.14f * cosY, packPos.second, packPos.third + 0.14f * sinY,
                rootTiltX, yawDeg, 0f,
                0.20f, 0.52f, 0.22f,
                0.85f, 0.88f, 0.94f, 1f, 0.1f
            )
            drawPart(
                shader, meshes.cube, vpMatrix,
                packPos.first + 0.14f * cosY, packPos.second, packPos.third - 0.14f * sinY,
                rootTiltX, yawDeg, 0f,
                0.20f, 0.52f, 0.22f,
                0.85f, 0.88f, 0.94f, 1f, 0.1f
            )
        } else {
            drawPart(
                shader, meshes.cube, vpMatrix,
                packPos.first, packPos.second, packPos.third,
                rootTiltX, yawDeg, 0f,
                0.42f, 0.45f, 0.20f,
                secondary[0], secondary[1], secondary[2], 1f, 0.05f
            )
        }

        // Head + Visor + Helmet/Hair
        val headPos = localOffset(0f, 1.62f, if (isSliding) 0.12f else -0.02f)
        drawPart(
            shader, meshes.sphere, vpMatrix,
            headPos.first, headPos.second, headPos.third,
            rootTiltX * 0.4f, yawDeg, 0f,
            0.44f, 0.44f, 0.44f,
            character.skinColor[0], character.skinColor[1], character.skinColor[2], 1f, 0.02f
        )
        // Visor
        val visorPos = localOffset(0f, 1.64f, -0.20f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            visorPos.first, visorPos.second, visorPos.third,
            rootTiltX * 0.4f, yawDeg, 0f,
            0.38f, 0.14f, 0.10f,
            accent[0], accent[1], accent[2], 1f, 0.55f
        )
        // Helmet / Cap dome
        val capPos = localOffset(0f, 1.78f, 0.02f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            capPos.first, capPos.second, capPos.third,
            rootTiltX * 0.4f, yawDeg, 0f,
            if (character.hasHelmet) 0.48f else 0.44f,
            if (character.hasHelmet) 0.22f else 0.16f,
            if (character.hasHelmet) 0.50f else 0.46f,
            primary[0], primary[1], primary[2], 1f, 0.08f
        )

        // Arms
        val leftArmPos = localOffset(-0.41f, 1.04f, 0f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            leftArmPos.first, leftArmPos.second, leftArmPos.third,
            rootTiltX + armSwing, yawDeg, 8f,
            0.18f, 0.56f, 0.18f,
            primary[0] * 0.9f, primary[1] * 0.9f, primary[2] * 0.9f, 1f, 0.04f
        )
        val rightArmPos = localOffset(0.41f, 1.04f, 0f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            rightArmPos.first, rightArmPos.second, rightArmPos.third,
            rootTiltX - armSwing, yawDeg, -8f,
            0.18f, 0.56f, 0.18f,
            primary[0] * 0.9f, primary[1] * 0.9f, primary[2] * 0.9f, 1f, 0.04f
        )

        // Legs + Shoes
        val leftLegPos = localOffset(-0.18f, 0.44f, 0f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            leftLegPos.first, leftLegPos.second, leftLegPos.third,
            rootTiltX * 0.5f + legSwing, yawDeg, 0f,
            0.22f, 0.58f, 0.22f,
            secondary[0], secondary[1], secondary[2], 1f, 0.02f
        )
        val rightLegPos = localOffset(0.18f, 0.44f, 0f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            rightLegPos.first, rightLegPos.second, rightLegPos.third,
            rootTiltX * 0.5f - legSwing, yawDeg, 0f,
            0.22f, 0.58f, 0.22f,
            secondary[0], secondary[1], secondary[2], 1f, 0.02f
        )

        // Shoes (glow bright neon when Super Sneakers active!)
        val shoeColor = if (sneakersActive) floatArrayOf(0.15f, 1.0f, 0.55f) else accent
        val shoeEmissive = if (sneakersActive) 0.55f else 0.12f
        val shoeScale = if (sneakersActive) 1.35f else 1.0f
        val leftShoePos = localOffset(-0.18f, 0.14f, -0.06f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            leftShoePos.first, leftShoePos.second, leftShoePos.third,
            legSwing * 0.5f, yawDeg, 0f,
            0.24f * shoeScale, 0.16f * shoeScale, 0.34f * shoeScale,
            shoeColor[0], shoeColor[1], shoeColor[2], 1f, shoeEmissive
        )
        val rightShoePos = localOffset(0.18f, 0.14f, -0.06f)
        drawPart(
            shader, meshes.cube, vpMatrix,
            rightShoePos.first, rightShoePos.second, rightShoePos.third,
            -legSwing * 0.5f, yawDeg, 0f,
            0.24f * shoeScale, 0.16f * shoeScale, 0.34f * shoeScale,
            shoeColor[0], shoeColor[1], shoeColor[2], 1f, shoeEmissive
        )

        // Translucent Energy Shield Dome
        if (shieldActive) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            drawPart(
                shader, meshes.sphere, vpMatrix,
                x, baseY + 0.95f, z,
                0f, yawDeg + runPhase * 30f, 0f,
                2.0f, 2.3f, 2.0f,
                0.15f, 0.92f, 1.0f, 0.28f, 0.65f
            )
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    private fun absBob(phase: Float, steady: Boolean): Float {
        if (steady) return 0.04f
        val s = sin(phase * 2f)
        return kotlin.math.abs(s) * 0.10f
    }

    private fun drawPart(
        shader: ShaderProgram,
        mesh: Mesh,
        vpMatrix: FloatArray,
        x: Float, y: Float, z: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        emissive: Float
    ) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (ry != 0f) Matrix.rotateM(model, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(model, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(model, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrix, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, model, 0)
        GLES20.glUniform4f(shader.uColor, r, g, b, a)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        mesh.bindAndDraw(shader.aPosition, shader.aNormal)
    }
}
