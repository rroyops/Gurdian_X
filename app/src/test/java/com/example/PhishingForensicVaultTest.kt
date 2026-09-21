package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.core.common.AppResult
import com.example.phishing.domain.PhishingIndicator
import com.example.phishing.domain.PhishingScanResult
import com.example.phishing.domain.PhishingScanner
import com.example.phishing.domain.RiskLevel
import com.example.phishing.ui.PhishingScannerScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhishingForensicVaultTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakeScanner : PhishingScanner {
        override suspend fun analyzeUrl(url: String): AppResult<PhishingScanResult> {
            return AppResult.Success(
                PhishingScanResult(
                    target = url,
                    riskLevel = RiskLevel.CRITICAL_THREAT,
                    indicators = listOf(
                        PhishingIndicator("SUSPICIOUS_TLD", "Suspicious TLD", "Uses .ru domain", RiskLevel.HIGH_RISK)
                    ),
                    explanation = "Detected lookalike domain targeting payment service",
                    recommendedAction = "Do not open or enter credentials",
                    analyzedAt = 1716123456000L
                )
            )
        }

        override suspend fun analyzeText(message: String): AppResult<PhishingScanResult> {
            return AppResult.Success(
                PhishingScanResult(
                    target = message,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    indicators = emptyList(),
                    explanation = "Message contains urgency cues",
                    recommendedAction = "Verify with sender",
                    analyzedAt = 1716123456000L
                )
            )
        }
    }

    @Test
    fun testScansThreatAndCommitsForensicsToVault() {
        var committedReport: PhishingScanResult? = null

        composeTestRule.setContent {
            GuardianXTheme {
                PhishingScannerScreen(
                    scanner = FakeScanner(),
                    onSaveThreatToVault = { committedReport = it }
                )
            }
        }

        // Screen is present
        composeTestRule.onNodeWithTag("phishing_scanner_screen").assertIsDisplayed()

        // Click preset malicious threat button to populate and scan
        composeTestRule.onNodeWithTag("button_phish_preset_malicious").performClick()
        composeTestRule.waitForIdle()

        // Wait for coroutine to complete and verdict card to appear
        composeTestRule.onNodeWithText("CRITICAL_THREAT VERDICT").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_save_threat_vault").performScrollTo().assertIsDisplayed()

        // Click Commit Forensic Report to Vault button
        composeTestRule.onNodeWithTag("button_save_threat_vault").performClick()
        composeTestRule.waitForIdle()

        // Verify callback received threat report
        assertTrue("Threat report must be committed to vault", committedReport != null)
        assertEquals("CRITICAL_THREAT", committedReport?.riskLevel?.name)

        // Verify button text shifts to committed state
        composeTestRule.onNodeWithText("Committed to Evidence Vault").performScrollTo().assertIsDisplayed()
    }
}
