package com.example.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun currentEpochSeconds(): Long
    fun isoTimestamp(): String
}

class SystemTimeProvider : TimeProvider {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

    override fun isoTimestamp(): String = synchronized(isoFormat) {
        isoFormat.format(Date(currentTimeMillis()))
    }
}
