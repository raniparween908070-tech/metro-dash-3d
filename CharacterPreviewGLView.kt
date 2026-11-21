package com.example.game3d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.CharacterDef
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class CharacterPreviewGLView(
    context: Context
) : GLSurfaceView(context) {

    @Volatile var currentCharacter: CharacterDef? = null
    @Volatile var currentOutfit: OutfitDef? = null
    @Volatile var currentHoverboard: HoverboardDef? = null
    @Volatile var showHoverboard: Boolean = false

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(PreviewRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private inner class PreviewRenderer : Renderer {
        private lateinit var shader: ShaderProgram
        private lateinit var meshes: MeshCatalog
        private val characterRenderer = CharacterModelRenderer()
        private val proj = FloatArray(16)
        private val view = FloatArray(16)
        private val vp = FloatArray(16)
        private var time = 0f
        private var lastNanos = System.nanoTime()

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            shader = ShaderProgram()
            meshes = MeshCatalog()
            lastNanos = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height.coerceAtLeast(1)
            Matrix.perspectiveM(proj, 0, 42f, ratio, 0.5f, 40f)
        }

        override fun onDrawFrame(gl: GL10?) {
            val now = System.nanoTime()
            val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
            lastNanos = now
            time += dt

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            val charDef = currentCharacter ?: return
            val boardDef = currentHoverboard ?: return

            shader.use()
            GLES20.glUniform3f(shader.uFogColor, 0.05f, 0.08f, 0.16f)
            GLES20.glUniform1f(shader.uFogDensity, 0.0f)
            GLES20.glUniform3f(shader.uLightDir, 0.4f, 0.9f, 0.6f)

            Matrix.setLookAtM(view, 0, 0f, 1.35f, 4.1f, 0f, 0.95f, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

            characterRenderer.drawRunner(
                shader = shader,
                meshes = meshes,
                vpMatrix = vp,
                x = 0f,
                y = 0f,
                z = 0f,
                yawDeg = 180f + time * 42f,
                runPhase = time * 4.5f,
                isJumping = false,
                isSliding = false,
                isFalling = false,
                fallTimer = 0f,
                jetpackActive = false,
                sneakersActive = false,
                hoverboardActive = showHoverboard,
                shieldActive = false,
                character = charDef,
                outfit = currentOutfit,
                hoverboard = boardDef,
                drawShadow = false
            )
        }
    }
}
