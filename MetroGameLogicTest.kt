package com.example

import com.example.data.PowerUpType
import com.example.data.StageGenerator
import com.example.game3d.GameWorldSimulation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroGameLogicTest {

    @Test
    fun `campaign has exactly 1000 stages of 5000 meters totaling 5 million meters`() {
        assertEquals(1000, StageGenerator.TOTAL_STAGES)
        assertEquals(5000, StageGenerator.STAGE_DISTANCE_METERS)
        assertEquals(5_000_000, StageGenerator.TOTAL_CAMPAIGN_METERS)

        var sumMeters = 0L
        for (stage in 1..1000) {
            val cfg = StageGenerator.configForStage(stage)
            assertEquals(stage, cfg.stageNumber)
            assertEquals(5000, cfg.targetDistanceMeters)
            sumMeters += cfg.targetDistanceMeters
        }
        assertEquals(5_000_000L, sumMeters)
        assertTrue(
            StageGenerator.configForStage(1000).baseSpeed >
                StageGenerator.configForStage(1).baseSpeed
        )
    }

    @Test
    fun `three lane movement jump slide and hoverboard controls work`() {
        val sim = GameWorldSimulation(
            stageConfig = StageGenerator.configForStage(1),
            powerUpLevels = PowerUpType.entries.associateWith { 1 }
        )

        assertEquals(0, sim.targetLane)
        sim.swipeLeft()
        assertEquals(-1, sim.targetLane)
        sim.swipeLeft() // clamp at -1
        assertEquals(-1, sim.targetLane)
        sim.swipeRight()
        sim.swipeRight()
        assertEquals(1, sim.targetLane)

        sim.swipeUp()
        assertTrue(sim.playerVy > 10f)
        sim.step(0.05f)
        assertTrue(sim.playerY > 0f)

        sim.swipeDown()
        assertTrue(sim.isSliding)

        assertTrue(sim.activateHoverboard())
        assertTrue((sim.activeTimers[PowerUpType.HOVERBOARD] ?: 0f) > 0f)
        assertFalse(sim.isGameOver)
    }
}
