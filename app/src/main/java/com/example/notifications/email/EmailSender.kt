package com.example.notifications.email

import com.example.core.common.AppResult

data class EmailPayload(
    val to: String,
    val subject: String,
    val bodyText: String,
    val bodyHtml: String? = null,
    val replyTo: String? = null
)

data class EmailSendResult(
    val isSuccess: Boolean,
    val messageId: String?,
    val statusCode: Int,
    val errorDetails: String? = null
)

interface EmailSender {
    suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult>
}
