package com.example.notifications.data

import android.content.Context
import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.TrustedContact
import com.example.core.common.AppResult
import com.example.core.error.AppError
import com.example.core.security.SecuritySanitizer
import com.example.emergency.domain.EmergencySession
import com.example.notifications.domain.DispatchMessage
import com.example.notifications.domain.EmergencyNotificationDispatcher
import com.example.notifications.email.EmailPayload
import com.example.notifications.email.EmailSender
import com.example.notifications.system.GuardianNotificationManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RealEmergencyNotificationDispatcher(
    private val context: Context,
    private val emailSender: EmailSender
) : EmergencyNotificationDispatcher {

    override suspend fun dispatchEmergencyAlert(
        session: EmergencySession,
        contacts: List<TrustedContact>
    ): AppResult<List<DispatchMessage>> {
        val now = System.currentTimeMillis()
        val latText = session.latitude?.let { "%.5f".format(it) } ?: "UNKNOWN"
        val lngText = session.longitude?.let { "%.5f".format(it) } ?: "UNKNOWN"
        val mapsLink = if (session.latitude != null && session.longitude != null) {
            "https://maps.google.com/?q=${session.latitude},${session.longitude}"
        } else {
            "Location unavailable"
        }

        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(session.startedAt))

        // If no contacts enrolled yet, default to Primary Guardian recipient so SOS trigger never drops
        val targetContacts = if (contacts.isEmpty()) {
            listOf(
                TrustedContact(
                    contactId = "primary_guardian_device",
                    userId = session.userId,
                    name = "Primary Guardian Contact",
                    phoneNumber = "+1 (555) 911-0001",
                    email = "oisheebiswasarmy07@gmail.com",
                    relationship = ContactRelationship.FAMILY,
                    isEmergencyRecipient = true,
                    priorityOrder = 1
                )
            )
        } else {
            contacts
        }

        val dispatches = coroutineScope {
            targetContacts.map { contact ->
                async {
                    val recipientEmail = contact.email?.trim().orEmpty()

                    var emailDeliveryStatus = "NOT_CONFIGURED"
                    var isEmailActuallyConfirmed = false

                    if (recipientEmail.isNotBlank()) {
                        if (SecuritySanitizer.isValidEmail(recipientEmail)) {
                            val subject = "🚨 URGENT: GuardianX Emergency SOS Broadcast from ${session.userId}"
                            val bodyText = buildString {
                                appendLine("GUARDIANX CRITICAL EMERGENCY ALERT")
                                appendLine("=================================")
                                appendLine("Recipient: ${contact.name}")
                                appendLine("User Identifier: ${session.userId}")
                                appendLine("Session ID: ${session.sessionId}")
                                appendLine("Trigger Type: ${session.triggerType.name}")
                                appendLine("Timestamp: $dateFormatted")
                                appendLine("Latitude: $latText")
                                appendLine("Longitude: $lngText")
                                appendLine("Live Navigation Trail: $mapsLink")
                                appendLine("")
                                appendLine("This is an authentic emergency dispatch triggered by the user via GuardianX.")
                                appendLine("Please verify immediately and contact local emergency dispatch if required.")
                            }

                            val bodyHtml = """
                                <div style="font-family: Arial, sans-serif; background-color: #0b132b; color: #f1f5f9; padding: 24px; border-radius: 8px;">
                                    <h2 style="color: #ff1744; margin-top: 0;">🚨 GUARDIANX CRITICAL EMERGENCY ALERT</h2>
                                    <p><strong>Recipient Guardian:</strong> ${contact.name}</p>
                                    <p><strong>User:</strong> ${session.userId}</p>
                                    <p><strong>Session ID:</strong> <code>${session.sessionId}</code></p>
                                    <p><strong>Timestamp:</strong> $dateFormatted</p>
                                    <p><strong>Trigger Mode:</strong> ${session.triggerType.name}</p>
                                    <div style="background-color: #131d38; padding: 16px; border-radius: 6px; border-left: 4px solid #00e5ff; margin: 16px 0;">
                                        <h3 style="color: #00e5ff; margin-top: 0;">Live GPS Coordinates</h3>
                                        <p><strong>Latitude:</strong> $latText &nbsp;|&nbsp; <strong>Longitude:</strong> $lngText</p>
                                        <p><a href="$mapsLink" style="background-color: #00e5ff; color: #000; padding: 8px 16px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;">Open Real-Time Map</a></p>
                                    </div>
                                    <p style="color: #94a3b8; font-size: 12px;">This alert was dispatched automatically by GuardianX Defense Core via Gmail SMTP.</p>
                                </div>
                            """.trimIndent()

                            val sendResult = emailSender.sendEmail(
                                EmailPayload(
                                    to = recipientEmail,
                                    subject = subject,
                                    bodyText = bodyText,
                                    bodyHtml = bodyHtml
                                )
                            )

                            emailDeliveryStatus = when (sendResult) {
                                is AppResult.Success -> {
                                    isEmailActuallyConfirmed = true
                                    "SENT (Confirmed by SMTP 250 OK)"
                                }
                                is AppResult.Error -> {
                                    isEmailActuallyConfirmed = false
                                    if (sendResult.error is AppError.NetworkError && (sendResult.error as AppError.NetworkError).isOffline) {
                                        "OFFLINE_QUEUED (Network unavailable - queued in Room)"
                                    } else {
                                        "FAILED (${sendResult.error.message})"
                                    }
                                }
                                is AppResult.Loading -> "PROCESSING"
                            }
                        } else {
                            emailDeliveryStatus = "INVALID_EMAIL_FORMAT ($recipientEmail)"
                        }
                    }

                    // Real Primary Device Push Notification (Offline and Online Continuity)
                    val notificationTitle = "🚨 SOS ACTIVE: Alert Dispatched"
                    val notificationBody = "Emergency trigger active for ${contact.name}. GPS: $latText, $lngText."
                    val pushSuccess = GuardianNotificationManager.showLocalSosNotification(
                        context = context,
                        notificationId = contact.contactId.hashCode(),
                        title = notificationTitle,
                        content = notificationBody
                    )
                    val pushStatus = if (pushSuccess) "DELIVERED_TO_SYSTEM" else "SYSTEM_NOTIFICATION_PENDING"

                    val messageSummary = "GUARDIANX SOS: Alerting ${contact.name}. GPS: $latText, $lngText. Maps: $mapsLink."

                    DispatchMessage(
                        recipientPhone = contact.phoneNumber,
                        recipientName = contact.name,
                        recipientEmail = contact.email,
                        messageText = messageSummary,
                        sessionId = session.sessionId,
                        timestamp = now,
                        // Only mark isDelivered if the email service actually confirmed sending
                        isDelivered = isEmailActuallyConfirmed,
                        emailStatus = emailDeliveryStatus,
                        pushStatus = pushStatus
                    )
                }
            }.awaitAll()
        }

        return AppResult.Success(dispatches)
    }
}
