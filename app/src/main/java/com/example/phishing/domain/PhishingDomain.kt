package com.example.phishing.domain

import com.example.core.common.AppResult

enum class RiskLevel {
    MINIMAL_OBSERVED_RISK,
    SUSPICIOUS,
    HIGH_RISK,
    CRITICAL_THREAT,
    INCONCLUSIVE
}

data class PhishingIndicator(
    val code: String,
    val title: String,
    val description: String,
    val severity: RiskLevel
)

data class PhishingScanResult(
    val target: String,
    val riskLevel: RiskLevel,
    val indicators: List<PhishingIndicator>,
    val explanation: String,
    val recommendedAction: String,
    val analyzedAt: Long,
    val confidenceNote: String = "Analysis is probabilistic based on heuristics and known patterns. Exercise caution."
)

interface PhishingScanner {
    suspend fun analyzeUrl(url: String): AppResult<PhishingScanResult>
    suspend fun analyzeText(message: String): AppResult<PhishingScanResult>
}
