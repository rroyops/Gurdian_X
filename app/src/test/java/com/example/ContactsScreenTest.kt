package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.TrustedContact
import com.example.contacts.ui.ContactsScreen
import com.example.ui.theme.GuardianXTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysContactAndTogglesEmergencyRecipient() {
        val testContact = TrustedContact(
            contactId = "con-111",
            userId = "LOCAL_USER",
            name = "Sarah Connor",
            phoneNumber = "+1-555-0199",
            email = "sarah@cyberdyne.sec",
            relationship = ContactRelationship.SECURITY_TEAM,
            isEmergencyRecipient = true,
            priorityOrder = 1
        )

        var toggledContactId: String? = null
        var deletedContactId: String? = null

        composeTestRule.setContent {
            GuardianXTheme {
                ContactsScreen(
                    contacts = listOf(testContact),
                    onAddContact = { _, _, _, _ -> },
                    onDeleteContact = { deletedContactId = it },
                    onToggleEmergencyRecipient = { toggledContactId = it }
                )
            }
        }

        // Verify Contacts Screen & Contact Card
        composeTestRule.onNodeWithTag("contacts_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("contact_card_con-111").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sarah Connor").assertIsDisplayed()
        composeTestRule.onNodeWithText("SECURITY_TEAM").assertIsDisplayed()
        composeTestRule.onNodeWithText("+1-555-0199").assertIsDisplayed()
        composeTestRule.onNodeWithText("CASCADE PRIORITY: TIER 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("SOS DISPATCH: ARMED").assertIsDisplayed()

        // Toggle Recipient State
        composeTestRule.onNodeWithTag("toggle_recipient_con-111").performClick()
        assertTrue("Recipient toggle must be invoked for con-111", toggledContactId == "con-111")

        // Delete Contact
        composeTestRule.onNodeWithTag("delete_contact_con-111").performClick()
        assertTrue("Delete contact must be invoked for con-111", deletedContactId == "con-111")
    }
}
