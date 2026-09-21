package com.example.core.logging

import android.util.Log

interface GuardianLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

class AndroidGuardianLogger : GuardianLogger {
    companion object {
        private const val PREFIX = "GuardianX."
        private val SENSITIVE_PATTERNS = listOf(
            Regex("""(?i)(password|token|secret|apiKey|authorization)=[^,\s&]+"""),
            Regex("""(?i)Bearer\s+[A-Za-z0-9-_.]+""")
        )

        fun sanitize(input: String): String {
            var result = input
            for (pattern in SENSITIVE_PATTERNS) {
                result = pattern.replace(result, "$1=[REDACTED]")
            }
            return result
        }
    }

    override fun d(tag: String, message: String) {
        Log.d(PREFIX + tag, sanitize(message))
    }

    override fun i(tag: String, message: String) {
        Log.i(PREFIX + tag, sanitize(message))
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(PREFIX + tag, sanitize(message), throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(PREFIX + tag, sanitize(message), throwable)
    }
}
