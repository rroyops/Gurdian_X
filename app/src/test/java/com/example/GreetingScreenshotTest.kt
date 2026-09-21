package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.emergency.ui.EmergencyScreen
import com.example.ui.theme.GuardianXTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun emergency_screen_screenshot() {
    composeTestRule.setContent {
      GuardianXTheme(darkTheme = true) {
        EmergencyScreen(
          activeSession = null,
          dispatchedAlerts = emptyList(),
          onTriggerSos = {},
          onResolveEmergency = {},
          onDiscreetAlert = {},
          onCountdownTick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/emergency_screen.png")
  }
}
