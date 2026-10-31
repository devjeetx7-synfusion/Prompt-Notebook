package com.synfusion.snipnote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal end-to-end smoke test of the real activity: branding renders, bottom navigation
 * works and Settings is reachable. Data-dependent assertions are avoided so the test is
 * stable on a fresh emulator.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRendersBrandingAndQuickActions() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SnipNote").assertIsDisplayed()
        composeRule.onNodeWithText("New prompt").assertIsDisplayed()
        composeRule.onAllNodesWithText("Quick capture")[0].assertIsDisplayed()
    }

    @Test
    fun bottomNavigationReachesSettingsAndCategories() {
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Settings")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("APPEARANCE").assertIsDisplayed()

        composeRule.onAllNodesWithText("Categories")[0].performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("AI Prompts").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("AI Prompts").assertIsDisplayed()
    }
}
