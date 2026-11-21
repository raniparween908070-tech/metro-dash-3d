package com.example.game3d

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import com.example.data.CharacterDef
import com.example.data.GraphicsQuality
import com.example.data.HoverboardDef
import com.example.data.OutfitDef
import com.example.data.StageConfig
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class MetroGLSurfaceView(
    context: Context,
    private val simulation: GameWorldSimulation,
    stageConfig: StageConfig,
    graphicsQuality: GraphicsQuality,
    targetFps: Int,
    character: CharacterDef,
    outfit: OutfitDef?,
    hoverboard: HoverboardDef,
    private val onDoubleTapHoverboard: () -> Unit,
    onHudTick: (HudSnapshot) -> Unit
) : GLSurfaceView(context) {

    private val renderer: Metro3DRenderer
    private var startX = 0f
    private var startY = 0f
    private var swipeHandled = false

    private val doubleTapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTapHoverboard()
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(2)
        renderer = Metro3DRenderer(
            simulation = simulation,
            stageConfig = stageConfig,
            graphicsQuality = graphicsQuality,
            targetFps = targetFps,
            character = character,
            outfit = outfit,
            hoverboard = hoverboard,
            onHudTick = onHudTick
        )
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        doubleTapDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                swipeHandled = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeHandled) return true
                val dx = event.x - startX
                val dy = event.y - startY
                val threshold = 42f
                if (abs(dx) > threshold || abs(dy) > threshold) {
                    swipeHandled = true
                    if (abs(dx) > abs(dy)) {
                        if (dx < 0f) simulation.swipeLeft() else simulation.swipeRight()
                    } else {
                        if (dy < 0f) simulation.swipeUp() else simulation.swipeDown()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
