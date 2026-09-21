package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.settings.ui.SettingsScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsSyncScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenDisplaysSyncQueueAndProcesses() {
        var syncClicked = false

        composeTestRule.setContent {
            GuardianXTheme {
                SettingsScreen(
                    auditCount = 42,
                    pendingSyncCount = 3,
                    onProcessSync = { syncClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline Safety & Cloud Sync Engine").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3 Record(s)").performScrollTo().assertIsDisplayed()

        // Trigger Sync
        composeTestRule.onNodeWithTag("button_process_sync").performScrollTo().performClick()
        assertTrue("Sync button callback must be triggered", syncClicked)
    }
}
