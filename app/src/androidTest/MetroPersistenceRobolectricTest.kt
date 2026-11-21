package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CurrencyType
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.data.PowerUpType
import com.example.data.PurchaseResult
import com.example.data.local.MetroDatabase
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MetroPersistenceRobolectricTest {

    private lateinit var db: MetroDatabase
    private lateinit var repo: GameRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MetroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(db.gameDao(), SettingsDataStore(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `stage unlock requires 5000m completion and shop purchases persist`() = runTest {
        repo.ensureInitialized()
        val initial = repo.profileFlow.first()
        assertEquals(1, initial.highestUnlockedStage)
        assertEquals(500, initial.coins)
        assertEquals(10, initial.diamonds)

        // Failed run at 2400m must NOT unlock Stage 2
        val failRun = repo.recordRunEnd(
            stageNumber = 1,
            distanceMeters = 2400,
            score = 28000L,
            coinsCollected = 50,
            diamondsCollected = 1,
            jumpsMade = 12,
            powerUpsActivated = 1,
            completedStage = false
        )
        assertEquals(null, failRun.unlockedNextStage)
        assertEquals(1, repo.profileFlow.first().highestUnlockedStage)

        // Completed run at 5000m unlocks Stage 2 and grants bonuses
        val winRun = repo.recordRunEnd(
            stageNumber = 1,
            distanceMeters = 5000,
            score = 72000L,
            coinsCollected = 1200,
            diamondsCollected = 5,
            jumpsMade = 30,
            powerUpsActivated = 3,
            completedStage = true
        )
        assertEquals(2, winRun.unlockedNextStage)
        assertEquals(2, repo.profileFlow.first().highestUnlockedStage)

        // Purchase Nova character (1500 coins)
        val nova = GameCatalog.characterById("nova")
        val res = repo.purchaseCharacter(nova)
        assertTrue(res is PurchaseResult.Success)
        assertTrue("char:nova" in repo.unlockedKeysFlow.first())

        // Upgrade Coin Magnet
        val upgRes = repo.upgradePowerUp(PowerUpType.MAGNET)
        assertTrue(upgRes is PurchaseResult.Success)
        assertEquals(2, repo.powerUpLevelsFlow.first()[PowerUpType.MAGNET])

        // Insufficient diamond purchase rejected cleanly
        val titan = GameCatalog.characterById("titan")
        val failBuy = repo.purchaseCharacter(titan)
        assertTrue(failBuy is PurchaseResult.InsufficientFunds)
        assertEquals(CurrencyType.DIAMONDS, (failBuy as PurchaseResult.InsufficientFunds).currency)
    }
}
