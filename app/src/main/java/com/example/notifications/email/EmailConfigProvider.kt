package com.example.notifications.email

import com.example.BuildConfig

data class SmtpConfig(
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    val username: String = "",
    val appPassword: String = "",
    val useSsl: Boolean = false,
    val senderEmail: String = "alerts@guardianx.safety"
)

object EmailConfigProvider {
    fun getSenderEmail(): String {
        return try {
            val email = BuildConfig.GUARDIANX_ALERT_SENDER_EMAIL
            if (email.isNotBlank() && email != "UNCONFIGURED") email else "alerts@guardianx.safety"
        } catch (_: Throwable) {
            "alerts@guardianx.safety"
        }
    }

    /**
     * Reads the API key / secret provided in GUARDIANX_ALERT_API_KEY.
     */
    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GUARDIANX_ALERT_API_KEY
            if (key == "UNCONFIGURED" || key.isBlank()) "" else key
        } catch (_: Throwable) {
            ""
        }
    }

    fun getSmtpConfig(): SmtpConfig {
        val host = try {
            val h = BuildConfig.GUARDIANX_SMTP_HOST
            if (h.isNotBlank() && h != "UNCONFIGURED") h else "smtp.gmail.com"
        } catch (_: Throwable) {
            "smtp.gmail.com"
        }

        val port = try {
            val portStr = BuildConfig.GUARDIANX_SMTP_PORT
            portStr.toIntOrNull() ?: 587
        } catch (_: Throwable) {
            587
        }

        val explicitSender = getSenderEmail()

        // Username resolution:
        // 1. Check GUARDIANX_GMAIL_USERNAME
        // 2. If blank/unconfigured, check if GUARDIANX_ALERT_SENDER_EMAIL is a valid email (e.g. rroy58230@gmail.com)
        val username = try {
            val user = BuildConfig.GUARDIANX_GMAIL_USERNAME
            if (user.isNotBlank() && user != "UNCONFIGURED") {
                user
            } else if (explicitSender.isNotBlank() && explicitSender != "alerts@guardianx.safety") {
                explicitSender
            } else {
                ""
            }
        } catch (_: Throwable) {
            if (explicitSender.isNotBlank() && explicitSender != "alerts@guardianx.safety") explicitSender else ""
        }

        // App Password resolution:
        // 1. Check GUARDIANX_GMAIL_APP_PASSWORD
        // 2. If blank/unconfigured, check GUARDIANX_ALERT_API_KEY (which is where the user enters the App Password)
        val appPassword = try {
            val pass = BuildConfig.GUARDIANX_GMAIL_APP_PASSWORD
            if (pass.isNotBlank() && pass != "UNCONFIGURED") {
                pass
            } else {
                val apiKey = getApiKey()
                if (apiKey.isNotBlank()) apiKey else ""
            }
        } catch (_: Throwable) {
            getApiKey()
        }

        val useSsl = try {
            BuildConfig.GUARDIANX_USE_SSL.equals("true", ignoreCase = true) || port == 465
        } catch (_: Throwable) {
            false
        }

        val sender = if (explicitSender.isNotBlank() && explicitSender != "alerts@guardianx.safety") {
            explicitSender
        } else if (username.isNotBlank()) {
            username
        } else {
            "alerts@guardianx.safety"
        }

        return SmtpConfig(
            host = host,
            port = port,
            username = username,
            appPassword = appPassword,
            useSsl = useSsl,
            senderEmail = sender
        )
    }

    fun isGmailConfigured(): Boolean {
        val config = getSmtpConfig()
        return config.username.isNotBlank() && config.appPassword.isNotBlank()
    }

    fun isApiConfigured(): Boolean {
        val key = getApiKey()
        // If it looks like a Gmail App Password (16 letters, optional spaces), it is for Gmail SMTP, not Resend REST API
        val cleanKey = key.replace(" ", "")
        val isGmailPasswordPattern = cleanKey.length == 16 && cleanKey.all { it in 'a'..'z' || it in 'A'..'Z' }
        if (isGmailPasswordPattern) {
            return false
        }
        return key.isNotBlank()
    }
}
