package com.example.core.security

import java.util.regex.Pattern

object SecuritySanitizer {
    private val EMAIL_REGEX = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
    )
    private val PHONE_CLEAN_REGEX = Regex("[^0-9+]")
    private val MAC_ADDRESS_REGEX = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
    private val URL_SCHEME_REGEX = Pattern.compile("^(https?|ftp)://", Pattern.CASE_INSENSITIVE)

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val trimmed = email.trim()
        if (trimmed.length > 254) return false
        return EMAIL_REGEX.matcher(trimmed).matches()
    }

    fun sanitizePhoneNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.replace(PHONE_CLEAN_REGEX, "").trim()
        return if (cleaned.startsWith("+")) {
            "+" + cleaned.substring(1).replace("+", "")
        } else {
            cleaned.replace("+", "")
        }
    }

    fun isValidPhoneNumber(phone: String?): Boolean {
        val sanitized = sanitizePhoneNumber(phone)
        val digitsOnly = sanitized.removePrefix("+")
        return digitsOnly.length in 7..15 && digitsOnly.all { it.isDigit() }
    }

    fun isValidMacAddress(mac: String?): Boolean {
        if (mac.isNullOrBlank()) return false
        return MAC_ADDRESS_REGEX.matcher(mac.trim()).matches()
    }

    fun sanitizeText(input: String?, maxLength: Int = 200): String {
        if (input.isNullOrBlank()) return ""
        return input.trim()
            .replace("\u0000", "")
            .take(maxLength)
    }

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (!URL_SCHEME_REGEX.matcher(trimmed).find()) return false
        return trimmed.length in 8..2048
    }
}
