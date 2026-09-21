package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.evidence.domain.EvidenceRecord
import com.example.evidence.domain.EvidenceType
import com.example.evidence.ui.EvidenceVaultScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EvidenceVaultScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysEvidenceRecordAndOpensCertificate() {
        val testRecord = EvidenceRecord(
            evidenceId = "ev-888",
            sessionId = "session-777",
            type = EvidenceType.LOCATION_TRAIL,
            localFilePath = "/vault/evidence_loc_888.dat",
            remoteStorageUri = null,
            sha256Checksum = "9876543210fedcba1234567890abcdef9876543210fedcba1234567890abcdef",
            fileSizeBytes = 1024,
            capturedAt = 1716123456000L
        )

        composeTestRule.setContent {
            GuardianXTheme {
                EvidenceVaultScreen(
                    evidenceList = listOf(testRecord),
                    onAddIncidentNote = {}
                )
            }
        }

        // Check screen and record card
        composeTestRule.onNodeWithTag("evidence_vault_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("evidence_record_ev-888").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOCATION TRAIL").assertIsDisplayed()

        // Tap card to inspect cryptographic certificate
        composeTestRule.onNodeWithTag("evidence_record_ev-888").performClick()

        // Verify certificate dialog components
        composeTestRule.onNodeWithTag("dialog_evidence_detail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forensic Record Certificate").assertIsDisplayed()
        composeTestRule.onNodeWithText("ev-888").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
    }
}
