package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MetroComposeUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `home screen displays after splash and navigates to stages shop settings help and privacy`() {
        composeRule.mainClock.advanceTimeBy(1400)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("play_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("stages_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("shop_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("missions_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_button").performScrollTo().assertIsDisplayed()

        // Navigate to 1000 Stages
        composeRule.onNodeWithTag("stages_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1000 STAGES").assertIsDisplayed()
        composeRule.onNodeWithTag("back_button").performClick()
        composeRule.waitForIdle()

        // Navigate to Shop
        composeRule.onNodeWithTag("shop_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("METRO SHOP").assertIsDisplayed()
        composeRule.onNodeWithTag("back_button").performClick()
        composeRule.waitForIdle()

        // Navigate to Privacy Policy
        composeRule.onNodeWithTag("privacy_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PRIVACY POLICY").assertIsDisplayed()
        composeRule.onNodeWithTag("back_button").performClick()
        composeRule.waitForIdle()

        // Navigate to Help & Support
        composeRule.onNodeWithTag("help_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("HELP & SUPPORT").assertIsDisplayed()
    }
}
