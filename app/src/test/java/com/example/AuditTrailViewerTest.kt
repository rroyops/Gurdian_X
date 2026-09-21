package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.core.logging.AuditAction
import com.example.core.logging.AuditEvent
import com.example.core.logging.AuditSeverity
import com.example.settings.ui.SettingsScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditTrailViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOpensAuditTrailViewerAndDisplaysEvents() {
        val testEvent = AuditEvent(
            eventId = "evt-123",
            timestamp = 1716123456000L,
            actorId = "LOCAL_USER",
            action = AuditAction.EMERGENCY_STARTED,
            severity = AuditSeverity.CRITICAL,
            resourceId = "session-999",
            details = "SOS triggered via HARDWARE_BLE_BUTTON",
            integrityHash = "a1b2c3d4e5f67890abcdef1234567890"
        )

        composeTestRule.setContent {
            GuardianXTheme {
                SettingsScreen(
                    auditCount = 1,
                    pendingSyncCount = 0,
                    recentAuditEvents = listOf(testEvent)
                )
            }
        }

        // Click on "View Cryptographic Audit Trail" button
        composeTestRule.onNodeWithTag("button_view_audit_trail").performScrollTo().performClick()

        // Audit Trail Viewer should now be displayed
        composeTestRule.onNodeWithTag("audit_trail_viewer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audit Log Vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMERGENCY_STARTED").assertIsDisplayed()
        composeTestRule.onNodeWithText("SOS triggered via HARDWARE_BLE_BUTTON").assertIsDisplayed()
        composeTestRule.onNodeWithText("SHA-256: a1b2c3d4e5f67890...").assertIsDisplayed()

        // Click the event card to open full cryptographic certificate dialog
        composeTestRule.onNodeWithTag("audit_event_evt-123").performClick()
        composeTestRule.waitForIdle()

        // Verify Certificate Dialog is shown
        composeTestRule.onNodeWithTag("audit_certificate_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cryptographic Certificate").assertIsDisplayed()
        composeTestRule.onNodeWithText("EVENT ID: evt-123").assertIsDisplayed()

        // Close certificate dialog
        composeTestRule.onNodeWithTag("button_dismiss_certificate").performClick()
        composeTestRule.waitForIdle()

        // Verify back button works to return to settings screen
        composeTestRule.onNodeWithTag("button_back_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }
}
