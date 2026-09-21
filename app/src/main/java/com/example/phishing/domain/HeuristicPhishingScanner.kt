package com.example.phishing.domain

import com.example.core.common.AppResult

class HeuristicPhishingScanner : PhishingScanner {

    override suspend fun analyzeUrl(url: String): AppResult<PhishingScanResult> {
        val lower = url.trim().lowercase()
        val indicators = mutableListOf<PhishingIndicator>()

        if (!lower.startsWith("https://")) {
            indicators.add(
                PhishingIndicator(
                    code = "NO_HTTPS",
                    title = "Unencrypted Transport",
                    description = "Destination does not enforce TLS/HTTPS encryption.",
                    severity = RiskLevel.SUSPICIOUS
                )
            )
        }

        val suspiciousKeywords = listOf("verify", "account", "update", "bank", "free", "login", "secure", "paypal")
        val matchCount = suspiciousKeywords.count { lower.contains(it) }
        if (matchCount >= 2) {
            indicators.add(
                PhishingIndicator(
                    code = "URGENT_CREDENTIAL_TRAP",
                    title = "Credential Harvesting Traps",
                    description = "URL contains multiple sensitive authentication trigger words.",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        val suspiciousTlds = listOf(".ru", ".tk", ".top", ".xyz", ".cn", ".fit")
        if (suspiciousTlds.any { lower.contains(it) }) {
            indicators.add(
                PhishingIndicator(
                    code = "HIGH_RISK_TLD",
                    title = "High Risk Domain Registry",
                    description = "Target domain uses top-level domains frequently abused by phishing campaigns.",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        val riskLevel = when {
            indicators.any { it.severity == RiskLevel.HIGH_RISK } -> RiskLevel.HIGH_RISK
            indicators.any { it.severity == RiskLevel.SUSPICIOUS } -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.MINIMAL_OBSERVED_RISK
        }

        val explanation = if (indicators.isEmpty()) {
            "No known malicious patterns, deceptive TLDs, or credential harvesting triggers detected."
        } else {
            "Detected ${indicators.size} suspicious threat indicator(s) matching known social engineering vectors."
        }

        val recommendation = when (riskLevel) {
            RiskLevel.HIGH_RISK -> "DO NOT OPEN. Block domain and avoid entering credentials or OTP codes."
            RiskLevel.SUSPICIOUS -> "Exercise extreme caution. Verify the sender through out-of-band communication."
            else -> "Destination appears benign under heuristic inspection."
        }

        return AppResult.Success(
            PhishingScanResult(
                target = url,
                riskLevel = riskLevel,
                indicators = indicators,
                explanation = explanation,
                recommendedAction = recommendation,
                analyzedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun analyzeText(message: String): AppResult<PhishingScanResult> {
        val lower = message.trim().lowercase()
        val indicators = mutableListOf<PhishingIndicator>()

        val urgencyKeywords = listOf("suspended", "urgent", "immediate action", "wire transfer", "unauthorized transaction")
        if (urgencyKeywords.any { lower.contains(it) }) {
            indicators.add(
                PhishingIndicator(
                    code = "URGENCY_COERCION",
                    title = "Coercive Psychological Pressure",
                    description = "Message attempts to create panic or urgent action to bypass rational verification.",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        val riskLevel = if (indicators.isNotEmpty()) RiskLevel.HIGH_RISK else RiskLevel.MINIMAL_OBSERVED_RISK

        return AppResult.Success(
            PhishingScanResult(
                target = message.take(50),
                riskLevel = riskLevel,
                indicators = indicators,
                explanation = if (indicators.isNotEmpty()) "Urgency and fear tactics detected in message body." else "No coercive language detected.",
                recommendedAction = if (indicators.isNotEmpty()) "Do not click any embedded links or reply with personal data." else "Message appears typical.",
                analyzedAt = System.currentTimeMillis()
            )
        )
    }
}
