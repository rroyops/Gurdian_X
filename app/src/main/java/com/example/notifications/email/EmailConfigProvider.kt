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

    private fun getBuildConfigField(fieldName: String): String? {
        val buildConfigVal = try {
            val field = BuildConfig::class.java.getField(fieldName)
            val value = field.get(null) as? String
            if (value != null && value.isNotBlank() && value != "UNCONFIGURED" && value != "alerts@guardianx.safety") value.trim() else null
        } catch (_: Throwable) {
            null
        }
        if (buildConfigVal != null) return buildConfigVal

        val envVal = System.getenv(fieldName)
        return if (envVal != null && envVal.isNotBlank() && envVal != "UNCONFIGURED" && envVal != "alerts@guardianx.safety") envVal.trim() else null
    }

    /**
     * Resolves the alert sender email address from any configured secret alias.
     */
    fun getSenderEmail(): String {
        return getBuildConfigField("GUARDIANX_GMAIL_USERNAME")
            ?: getBuildConfigField("GUARDIANX_ALERT_SENDER_EMAIL")
            ?: getBuildConfigField("GUARDIANX_ALERT_SEND")
            ?: "rroy58230@gmail.com"
    }

    /**
     * Reads the API key / credential from any configured secret alias.
     */
    fun getApiKey(): String {
        return getBuildConfigField("GUARDIANX_ALERT_API_K")
            ?: getBuildConfigField("GUARDIANX_ALERT_API_KEY")
            ?: getBuildConfigField("GUARDIANX_GMAIL_APP_PASSWORD")
            ?: ""
    }

    /**
     * Constructs the full SMTP configuration for direct Gmail dispatch.
     */
    fun getSmtpConfig(): SmtpConfig {
        val host = getBuildConfigField("GUARDIANX_SMTP_HOST") ?: "smtp.gmail.com"
        val portStr = getBuildConfigField("GUARDIANX_SMTP_PORT") ?: "587"
        val port = portStr.toIntOrNull() ?: 587

        val sender = getSenderEmail()
        val username = getBuildConfigField("GUARDIANX_GMAIL_USERNAME")
            ?: getBuildConfigField("GUARDIANX_ALERT_SENDER_EMAIL")
            ?: getBuildConfigField("GUARDIANX_ALERT_SEND")
            ?: (if (sender.contains("@")) sender else "rroy58230@gmail.com")

        val appPassword = getBuildConfigField("GUARDIANX_GMAIL_APP_PASSWORD")
            ?: getBuildConfigField("GUARDIANX_ALERT_API_K")
            ?: getBuildConfigField("GUARDIANX_ALERT_API_KEY")
            ?: ""

        val useSsl = getBuildConfigField("GUARDIANX_USE_SSL").equals("true", ignoreCase = true) || port == 465

        val resolvedSender = if (sender.contains("@")) sender else username

        return SmtpConfig(
            host = host,
            port = port,
            username = username,
            appPassword = appPassword,
            useSsl = useSsl,
            senderEmail = resolvedSender
        )
    }

    fun isGmailConfigured(): Boolean {
        val config = getSmtpConfig()
        return config.username.isNotBlank() && config.appPassword.isNotBlank()
    }

    /**
     * Only consider HTTP REST API (Resend) configured if an actual Resend API key is present.
     * Google Gmail App Passwords will NOT trigger HTTP REST API.
     */
    fun isApiConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key.startsWith("re_")
    }
}
