package com.example

import com.example.core.common.AppResult
import com.example.phishing.domain.EnhancedPhishingScanner
import com.example.phishing.domain.RiskLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnhancedPhishingScannerTest {

    private lateinit var scanner: EnhancedPhishingScanner

    @Before
    fun setUp() {
        scanner = EnhancedPhishingScanner()
    }

    @Test
    fun testDetectsHomoglyphTyposquatting() = runTest {
        val result = scanner.analyzeUrl("https://paypa1-security.com/login")
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(RiskLevel.CRITICAL_THREAT, data.riskLevel)
        assertTrue(data.indicators.any { it.code == "TYPOSQUATTING_HOMOGLYPH" })
    }

    @Test
    fun testDetectsRawIpHostname() = runTest {
        val result = scanner.analyzeUrl("http://192.168.1.50/verify-account")
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(RiskLevel.CRITICAL_THREAT, data.riskLevel)
        assertTrue(data.indicators.any { it.code == "RAW_IP_HOSTNAME" })
    }

    @Test
    fun testDetectsCoerciveSmsMessage() = runTest {
        val sms = "URGENT: Your account has an unauthorized transaction! Immediate action required to prevent suspension: https://mysecure-bank.xyz"
        val result = scanner.analyzeText(sms)
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(RiskLevel.HIGH_RISK, data.riskLevel)
        assertTrue(data.indicators.any { it.code == "PSYCHOLOGICAL_COERCION" })
        assertTrue(data.indicators.any { it.code == "HIGH_RISK_REGISTRY" })
    }

    @Test
    fun testRecognizesSafeUrl() = runTest {
        val safeUrl = "https://developer.android.com/jetpack"
        val result = scanner.analyzeUrl(safeUrl)
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(RiskLevel.MINIMAL_OBSERVED_RISK, data.riskLevel)
        assertTrue(data.indicators.isEmpty())
    }
}
