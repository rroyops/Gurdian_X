package com.example.notifications.domain

import com.example.core.common.AppResult
import kotlinx.coroutines.flow.Flow

enum class DeliveryStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    SENT,
    DELIVERED,
    FAILED,
    RETRYING,
    UNAVAILABLE
}

enum class AlertChannel {
    FCM_PUSH,
    SMS_DIRECT,
    EMAIL_ALERT,
    HARDWARE_BUZZER
}

data class AlertDispatch(
    val alertId: String,
    val sessionId: String,
    val recipientId: String,
    val recipientAddress: String,
    val channel: AlertChannel,
    val status: DeliveryStatus,
    val queuedAt: Long,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val failureReason: String? = null,
    val retryCount: Int = 0
)

interface NotificationGateway {
    suspend fun queueAlert(dispatch: AlertDispatch): AppResult<AlertDispatch>
    suspend fun updateDeliveryStatus(alertId: String, status: DeliveryStatus, details: String? = null): AppResult<Unit>
    fun observeDispatchesForSession(sessionId: String): Flow<List<AlertDispatch>>
}
