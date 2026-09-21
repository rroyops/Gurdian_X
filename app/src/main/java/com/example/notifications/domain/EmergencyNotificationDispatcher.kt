package com.example.notifications.domain

import com.example.contacts.domain.TrustedContact
import com.example.core.common.AppResult
import com.example.emergency.domain.EmergencySession

data class DispatchMessage(
    val recipientPhone: String,
    val recipientName: String,
    val recipientEmail: String? = null,
    val messageText: String,
    val sessionId: String,
    val timestamp: Long,
    val isDelivered: Boolean = false,
    val emailStatus: String = "NOT_CONFIGURED", // "SENT", "FAILED", "INVALID_EMAIL", "NOT_CONFIGURED"
    val pushStatus: String = "NOT_CONFIGURED"  // "SENT", "PERMISSION_DENIED", "NOT_CONFIGURED"
)

interface EmergencyNotificationDispatcher {
    suspend fun dispatchEmergencyAlert(
        session: EmergencySession,
        contacts: List<TrustedContact>
    ): AppResult<List<DispatchMessage>>
}
