package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.TrustedContact
import com.example.core.common.AppResult
import com.example.emergency.domain.EmergencySession
import com.example.emergency.domain.EmergencyState
import com.example.emergency.domain.EmergencyTriggerType
import com.example.notifications.data.RealEmergencyNotificationDispatcher
import com.example.notifications.email.EmailPayload
import com.example.notifications.email.EmailSendResult
import com.example.notifications.email.EmailSender
import com.example.notifications.email.RestEmailSender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealEmergencyAndGmailAlertTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // 1. Mock/Fake Email Sender that records real transmissions without external network calls
    class FakeRecordingEmailSender(
        var shouldSucceed: Boolean = true,
        var responseCode: Int = 200,
        var failureMessage: String = "SMTP Relay Rejected"
    ) : EmailSender {
        val sentPayloads = mutableListOf<EmailPayload>()

        override suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult> {
            sentPayloads.add(payload)
            return if (shouldSucceed) {
                AppResult.Success(
                    EmailSendResult(
                        isSuccess = true,
                        messageId = "msg_live_${System.currentTimeMillis()}_${payload.to.hashCode()}",
                        statusCode = responseCode
                    )
                )
            } else {
                AppResult.Error(
                    com.example.core.error.AppError.EmailError(
                        recipient = payload.to,
                        message = failureMessage,
                        errorCode = "HTTP_$responseCode"
                    )
                )
            }
        }
    }

    // TEST 1: SOS -> Guardian push notification & Gmail alert dispatch
    @Test
    fun testSosEmergencyNotificationAndGmailAlertDispatch() = runBlocking {
        val fakeEmailSender = FakeRecordingEmailSender(shouldSucceed = true)
        val dispatcher = RealEmergencyNotificationDispatcher(context, fakeEmailSender)

        val session = EmergencySession(
            sessionId = "sess_sos_9918",
            userId = "USR_ALICE_DEV",
            status = EmergencyState.ACTIVE,
            triggerType = EmergencyTriggerType.MANUAL_SOS_BUTTON,
            startedAt = System.currentTimeMillis(),
            latitude = 37.77492,
            longitude = -122.41942,
            locationAccuracy = 4.5f
        )

        val guardianContact = TrustedContact(
            contactId = "ct_guardian_bob",
            userId = "USR_ALICE_DEV",
            name = "Guardian Bob",
            phoneNumber = "+14155552671",
            email = "bob.guardian@safety.org",
            relationship = ContactRelationship.FAMILY,
            isEmergencyRecipient = true,
            priorityOrder = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val result = dispatcher.dispatchEmergencyAlert(session, listOf(guardianContact))
        assertTrue("Dispatch alert must succeed", result is AppResult.Success)
        val dispatches = (result as AppResult.Success).data
        assertEquals(1, dispatches.size)

        val dispatch = dispatches[0]
        assertEquals("Guardian Bob", dispatch.recipientName)
        assertEquals("bob.guardian@safety.org", dispatch.recipientEmail)
        assertTrue("Email status should indicate success with message ID", dispatch.emailStatus.startsWith("SENT"))
        assertTrue("Push notification status should indicate system delivery", dispatch.pushStatus == "DELIVERED_TO_SYSTEM")
        assertTrue("Overall delivery flag should be true", dispatch.isDelivered)

        // Verify sent payload details
        assertEquals(1, fakeEmailSender.sentPayloads.size)
        val sentEmail = fakeEmailSender.sentPayloads[0]
        assertEquals("bob.guardian@safety.org", sentEmail.to)
        assertTrue(sentEmail.subject.contains("Emergency SOS Broadcast"))
        assertTrue(sentEmail.bodyText.contains("USR_ALICE_DEV"))
        assertTrue(sentEmail.bodyText.contains("37.77492"))
        assertTrue(sentEmail.bodyText.contains("-122.41942"))
        assertTrue(sentEmail.bodyText.contains("https://maps.google.com/?q=37.77492,-122.41942"))
    }

    // TEST 2: Invalid guardian email format handling
    @Test
    fun testInvalidGuardianEmailHandling() = runBlocking {
        val fakeEmailSender = FakeRecordingEmailSender()
        val dispatcher = RealEmergencyNotificationDispatcher(context, fakeEmailSender)

        val session = EmergencySession(
            sessionId = "sess_sos_9919",
            userId = "USR_ALICE_DEV",
            status = EmergencyState.ACTIVE,
            triggerType = EmergencyTriggerType.MANUAL_SOS_BUTTON,
            startedAt = System.currentTimeMillis(),
            latitude = 37.77492,
            longitude = -122.41942,
            locationAccuracy = 4.5f
        )

        val contactWithBadEmail = TrustedContact(
            contactId = "ct_bad_email",
            userId = "USR_ALICE_DEV",
            name = "Guardian Bad Email",
            phoneNumber = "+14155552672",
            email = "bad-email-format@@notvalid",
            relationship = ContactRelationship.FRIEND,
            isEmergencyRecipient = true,
            priorityOrder = 2,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val result = dispatcher.dispatchEmergencyAlert(session, listOf(contactWithBadEmail))
        assertTrue(result is AppResult.Success)
        val dispatches = (result as AppResult.Success).data
        val dispatch = dispatches[0]

        assertTrue(dispatch.emailStatus.startsWith("INVALID_EMAIL_FORMAT"))
        assertEquals(0, fakeEmailSender.sentPayloads.size) // Never dispatched to bad email
    }

    // TEST 3: Email delivery service failure handling (e.g. rate limit / network / auth error)
    @Test
    fun testEmailServiceRejectionHandling() = runBlocking {
        val failingSender = FakeRecordingEmailSender(
            shouldSucceed = false,
            responseCode = 429,
            failureMessage = "Rate limit exceeded on emergency sender"
        )
        val dispatcher = RealEmergencyNotificationDispatcher(context, failingSender)

        val session = EmergencySession(
            sessionId = "sess_sos_9920",
            userId = "USR_ALICE_DEV",
            status = EmergencyState.ACTIVE,
            triggerType = EmergencyTriggerType.MANUAL_SOS_BUTTON,
            startedAt = System.currentTimeMillis()
        )

        val contact = TrustedContact(
            contactId = "ct_valid",
            userId = "USR_ALICE_DEV",
            name = "Guardian Charlie",
            phoneNumber = "+14155552673",
            email = "charlie@safety.org",
            relationship = ContactRelationship.COLLEAGUE,
            isEmergencyRecipient = true,
            priorityOrder = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val result = dispatcher.dispatchEmergencyAlert(session, listOf(contact))
        assertTrue(result is AppResult.Success)
        val dispatch = (result as AppResult.Success).data[0]

        assertTrue("Email status should report failure", dispatch.emailStatus.startsWith("FAILED"))
        assertTrue("Email status should include error message", dispatch.emailStatus.contains("Rate limit exceeded"))
    }

    // TEST 4: RestEmailSender unconfigured API key security guarantee
    @Test
    fun testRestEmailSenderFailsGracefullyWhenUnconfigured() = runBlocking {
        // Without an API key, RestEmailSender must return AppResult.Error instead of silently faking success
        val restSender = RestEmailSender(
            apiKeyProvider = { "" },
            senderEmailProvider = { "alerts@guardianx.safety" }
        )

        val payload = EmailPayload(
            to = "guardian@example.com",
            subject = "SOS Alert",
            bodyText = "Emergency active"
        )

        val result = restSender.sendEmail(payload)
        assertTrue("RestEmailSender must return AppResult.Error when unconfigured", result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is com.example.core.error.AppError.EmailError)
        assertTrue(error.message.contains("CREDENTIALS_MISSING") || error.message.contains("unconfigured"))
    }

    // TEST 5: Location unavailable fallback
    @Test
    fun testAlertWithUnavailableLocation() = runBlocking {
        val fakeEmailSender = FakeRecordingEmailSender()
        val dispatcher = RealEmergencyNotificationDispatcher(context, fakeEmailSender)

        val sessionWithoutLocation = EmergencySession(
            sessionId = "sess_no_loc",
            userId = "USR_BOB",
            status = EmergencyState.ACTIVE,
            triggerType = EmergencyTriggerType.HARDWARE_BLE_BUTTON,
            startedAt = System.currentTimeMillis(),
            latitude = null,
            longitude = null
        )

        val contact = TrustedContact(
            contactId = "ct_dan",
            userId = "USR_BOB",
            name = "Guardian Dan",
            phoneNumber = "+14155552674",
            email = "dan@safety.org",
            relationship = ContactRelationship.SECURITY_TEAM,
            isEmergencyRecipient = true,
            priorityOrder = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val result = dispatcher.dispatchEmergencyAlert(sessionWithoutLocation, listOf(contact))
        assertTrue(result is AppResult.Success)

        val sentEmail = fakeEmailSender.sentPayloads[0]
        assertTrue(sentEmail.bodyText.contains("UNKNOWN"))
        assertTrue(sentEmail.bodyText.contains("Location unavailable"))
    }

    // TEST 6: SecureSmtpEmailSender rejects unconfigured credentials gracefully
    @Test
    fun testSecureSmtpEmailSenderFailsGracefullyWhenUnconfigured() = runBlocking {
        val unconfiguredSmtp = com.example.notifications.email.SecureSmtpEmailSender(
            configProvider = {
                com.example.notifications.email.SmtpConfig(
                    username = "",
                    appPassword = ""
                )
            }
        )

        val payload = EmailPayload(
            to = "guardian@example.com",
            subject = "SOS Alert",
            bodyText = "Emergency active"
        )

        val result = unconfiguredSmtp.sendEmail(payload)
        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is com.example.core.error.AppError.EmailError)
        assertEquals("SMTP_AUTH_MISSING", (error as com.example.core.error.AppError.EmailError).errorCode)
    }

    // TEST 7: CompositeEmergencyEmailSender routes to SMTP when armed
    @Test
    fun testCompositeSenderRoutesToSmtpWhenGmailArmed() = runBlocking {
        val fakeSmtp = FakeRecordingEmailSender(shouldSucceed = true, responseCode = 250)
        val fakeRest = FakeRecordingEmailSender(shouldSucceed = true, responseCode = 200)

        val composite = com.example.notifications.email.CompositeEmergencyEmailSender(
            smtpSender = fakeSmtp,
            restSender = fakeRest,
            configStatusProvider = { Pair(true, false) }
        )

        val payload = EmailPayload(
            to = "guardian@example.com",
            subject = "SOS Alert",
            bodyText = "Emergency active"
        )

        val result = composite.sendEmail(payload)
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeSmtp.sentPayloads.size)
        assertEquals(0, fakeRest.sentPayloads.size)
    }

    // TEST 8: CompositeEmergencyEmailSender falls back to REST when SMTP fails and REST is armed
    @Test
    fun testCompositeSenderFallsBackToRestWhenSmtpFails() = runBlocking {
        val failingSmtp = FakeRecordingEmailSender(shouldSucceed = false, responseCode = 535, failureMessage = "Auth Failed")
        val fallbackRest = FakeRecordingEmailSender(shouldSucceed = true, responseCode = 200)

        val composite = com.example.notifications.email.CompositeEmergencyEmailSender(
            smtpSender = failingSmtp,
            restSender = fallbackRest,
            configStatusProvider = { Pair(true, true) }
        )

        val payload = EmailPayload(
            to = "guardian@example.com",
            subject = "SOS Alert",
            bodyText = "Emergency active"
        )

        val result = composite.sendEmail(payload)
        assertTrue(result is AppResult.Success)
        assertEquals(1, failingSmtp.sentPayloads.size)
        assertEquals(1, fallbackRest.sentPayloads.size)
    }
}
