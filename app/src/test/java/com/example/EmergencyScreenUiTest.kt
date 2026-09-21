package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.emergency.ui.EmergencyScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEmergencyScreenRendersIdleState() {
        var triggered = false

        composeTestRule.setContent {
            GuardianXTheme {
                EmergencyScreen(
                    activeSession = null,
                    dispatchedAlerts = emptyList(),
                    onTriggerSos = { triggered = true },
                    onResolveEmergency = {},
                    onDiscreetAlert = {},
                    onCountdownTick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("emergency_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tactical_sos_button_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("SHIELD IDLE • STANDBY").assertIsDisplayed()

        // Click SOS Trigger
        composeTestRule.onNodeWithTag("sos_trigger_touch_target").performClick()
        assertTrue("SOS trigger callback must be fired", triggered)
    }
}
