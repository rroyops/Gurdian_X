package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.devices.domain.DeviceType
import com.example.devices.domain.GuardianDevice
import com.example.devices.ui.DevicesScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DevicesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysEnrolledDeviceAndTriggersActions() {
        val testDevice = GuardianDevice(
            deviceId = "dev-456",
            userId = "LOCAL_USER",
            name = "ESP32-SOS-BEACON",
            macAddress = "AA:BB:CC:11:22:33",
            deviceType = DeviceType.ESP32_BLE_BEACON,
            enrollmentDate = 1716123456000L,
            isPaired = true,
            batteryLevel = 92
        )

        var heartbeatPinged = false
        var discreetSosTriggered = false

        composeTestRule.setContent {
            GuardianXTheme {
                DevicesScreen(
                    devices = listOf(testDevice),
                    onPairDevice = { _, _, _ -> },
                    onUnpairDevice = {},
                    onSendHeartbeat = { heartbeatPinged = true },
                    onDiscreetTrigger = { discreetSosTriggered = true }
                )
            }
        }

        // Verify Device Screen & Card
        composeTestRule.onNodeWithTag("devices_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_card_dev-456").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESP32-SOS-BEACON").assertIsDisplayed()
        composeTestRule.onNodeWithText("MAC: AA:BB:CC:11:22:33").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 92% BATTERY").assertIsDisplayed()

        // Click Continuity Ping button
        composeTestRule.onNodeWithTag("button_ping_heartbeat_dev-456").performClick()
        assertTrue("Heartbeat callback must be invoked", heartbeatPinged)

        // Click Trigger SOS button
        composeTestRule.onNodeWithTag("button_discreet_sos_dev-456").performClick()
        assertTrue("Discreet SOS callback must be invoked", discreetSosTriggered)
    }
}
