package com.example.notifications.email

import com.example.core.common.AppResult
import com.example.core.error.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composite Email Dispatcher that automatically routes dispatches through:
 * 1. Gmail SMTP Relay (if configured with Google App Password)
 * 2. Or HTTP REST API Delivery Relay (if configured with GUARDIANX_ALERT_API_KEY)
 *
 * If both or neither are configured, provides actionable diagnostic feedback in compliance
 * with zero-mock policies.
 */
class CompositeEmergencyEmailSender(
    private val smtpSender: EmailSender,
    private val restSender: EmailSender,
    private val configStatusProvider: () -> Pair<Boolean, Boolean> = {
        Pair(EmailConfigProvider.isGmailConfigured(), EmailConfigProvider.isApiConfigured())
    }
) : EmailSender {

    override suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult> = withContext(Dispatchers.IO) {
        val (isGmailConfigured, isApiConfigured) = configStatusProvider()

        // 1. Prefer direct Gmail SMTP if user configured Gmail credentials
        if (isGmailConfigured) {
            val smtpResult = smtpSender.sendEmail(payload)
            if (smtpResult is AppResult.Success) {
                return@withContext smtpResult
            }
            // If SMTP failed and REST API is configured, attempt fallback
            if (isApiConfigured) {
                return@withContext restSender.sendEmail(payload)
            }
            return@withContext smtpResult
        }

        // 2. Otherwise use REST API Relay if configured
        if (isApiConfigured) {
            return@withContext restSender.sendEmail(payload)
        }

        // 3. Unconfigured: Provide clear guidance on how to arm Gmail or REST in AI Studio Secrets
        AppResult.Error(
            AppError.EmailError(
                recipient = payload.to,
                message = "Gmail/Email Service Unconfigured: Set GUARDIANX_GMAIL_USERNAME & GUARDIANX_GMAIL_APP_PASSWORD (for direct Gmail dispatch) or GUARDIANX_ALERT_API_KEY in the AI Studio Secrets panel.",
                errorCode = "NO_CREDENTIALS_ARMED"
            )
        )
    }
}
