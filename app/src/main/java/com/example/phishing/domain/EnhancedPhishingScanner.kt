package com.example.phishing.domain

import com.example.core.common.AppResult

class EnhancedPhishingScanner : PhishingScanner {

    private val suspiciousTlds = listOf(".ru", ".tk", ".top", ".xyz", ".cn", ".fit", ".ml", ".ga", ".cf", ".gq")
    private val sensitiveKeywords = listOf("verify", "account", "update", "bank", "free", "login", "secure", "paypal", "wallet", "crypto", "password", "billing")
    private val brandLookalikes = listOf("g00gle", "paypa1", "micros0ft", "app1e", "faceb00k", "amaz0n", "netf1ix", "wellsfarg0", "chase-secure", "bofa-online")
    private val coercivePhrases = listOf("suspended", "urgent", "immediate action", "wire transfer", "unauthorized transaction", "police warrant", "arrest", "tax penalty", "compromised")

    override suspend fun analyzeUrl(url: String): AppResult<PhishingScanResult> {
        val cleanUrl = url.trim()
        val lower = cleanUrl.lowercase()
        val indicators = mutableListOf<PhishingIndicator>()

        // 1. Check Protocol & Encryption
        if (!lower.startsWith("https://")) {
            indicators.add(
                PhishingIndicator(
                    code = "NO_HTTPS",
                    title = "Unencrypted Protocol",
                    description = "Destination does not enforce TLS/HTTPS encryption, exposing traffic to interception.",
                    severity = RiskLevel.SUSPICIOUS
                )
            )
        }

        // 2. Typosquatting and Brand Homoglyphs / Lookalikes
        if (brandLookalikes.any { lower.contains(it) }) {
            indicators.add(
                PhishingIndicator(
                    code = "TYPOSQUATTING_HOMOGLYPH",
                    title = "Deceptive Brand Imitation",
                    description = "Target domain employs visual homoglyphs or typosquatting imitating recognized services.",
                    severity = RiskLevel.CRITICAL_THREAT
                )
            )
        }

        // 3. Sensitive Authentication Keyword Traps
        val matchCount = sensitiveKeywords.count { lower.contains(it) }
        if (matchCount >= 2) {
            indicators.add(
                PhishingIndicator(
                    code = "CREDENTIAL_HARVEST_TRAP",
                    title = "Credential Harvesting Traps",
                    description = "URL contains multiple sensitive authentication and banking trigger tokens ($matchCount detected).",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        // 4. Malicious Top-Level Domains (TLDs)
        if (suspiciousTlds.any { lower.contains(it) }) {
            indicators.add(
                PhishingIndicator(
                    code = "HIGH_RISK_REGISTRY",
                    title = "Abused High-Risk Registry",
                    description = "Domain is registered under a top-level domain frequently utilized in disposable phishing setups.",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        // 5. IP Address As Hostname (e.g. http://192.168.1.1/login)
        val ipPattern = Regex("""^https?://\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(/.*)?$""")
        if (ipPattern.matches(lower)) {
            indicators.add(
                PhishingIndicator(
                    code = "RAW_IP_HOSTNAME",
                    title = "Raw IP Hostname",
                    description = "Destination uses a numeric IP address rather than a verified domain name to evade reputation filters.",
                    severity = RiskLevel.CRITICAL_THREAT
                )
            )
        }

        val riskLevel = when {
            indicators.any { it.severity == RiskLevel.CRITICAL_THREAT } -> RiskLevel.CRITICAL_THREAT
            indicators.any { it.severity == RiskLevel.HIGH_RISK } -> RiskLevel.HIGH_RISK
            indicators.any { it.severity == RiskLevel.SUSPICIOUS } -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.MINIMAL_OBSERVED_RISK
        }

        val explanation = if (indicators.isEmpty()) {
            "No known malicious patterns, homoglyphs, or credential harvesting triggers detected."
        } else {
            "Detected ${indicators.size} distinct threat signature(s) matching known social engineering attack models."
        }

        val recommendation = when (riskLevel) {
            RiskLevel.CRITICAL_THREAT -> "IMMEDIATE THREAT. DO NOT VISIT. Discard message and block sender domain immediately."
            RiskLevel.HIGH_RISK -> "DO NOT OPEN. High probability of credential harvesting. Avoid entering passwords or OTPs."
            RiskLevel.SUSPICIOUS -> "Exercise extreme caution. Verify the sender through official out-of-band communication."
            else -> "Destination appears benign under multilayered heuristic inspection."
        }

        return AppResult.Success(
            PhishingScanResult(
                target = cleanUrl,
                riskLevel = riskLevel,
                indicators = indicators,
                explanation = explanation,
                recommendedAction = recommendation,
                analyzedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun analyzeText(message: String): AppResult<PhishingScanResult> {
        val cleanMsg = message.trim()
        val lower = cleanMsg.lowercase()
        val indicators = mutableListOf<PhishingIndicator>()

        val foundCoercive = coercivePhrases.filter { lower.contains(it) }
        if (foundCoercive.isNotEmpty()) {
            indicators.add(
                PhishingIndicator(
                    code = "PSYCHOLOGICAL_COERCION",
                    title = "Coercive Urgency Pressure",
                    description = "Message leverages artificial urgency (${foundCoercive.joinToString()}) designed to trigger panic and bypass verification.",
                    severity = RiskLevel.HIGH_RISK
                )
            )
        }

        // Check for embedded suspicious URLs within text
        val embeddedUrlRegex = Regex("""https?://[^\s]+""")
        val embeddedUrls = embeddedUrlRegex.findAll(cleanMsg).map { it.value }.toList()
        for (url in embeddedUrls) {
            val urlScan = analyzeUrl(url)
            if (urlScan is AppResult.Success && urlScan.data.riskLevel != RiskLevel.MINIMAL_OBSERVED_RISK) {
                indicators.addAll(urlScan.data.indicators)
            }
        }

        val riskLevel = when {
            indicators.any { it.severity == RiskLevel.CRITICAL_THREAT } -> RiskLevel.CRITICAL_THREAT
            indicators.any { it.severity == RiskLevel.HIGH_RISK } -> RiskLevel.HIGH_RISK
            indicators.any { it.severity == RiskLevel.SUSPICIOUS } -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.MINIMAL_OBSERVED_RISK
        }

        return AppResult.Success(
            PhishingScanResult(
                target = cleanMsg.take(60),
                riskLevel = riskLevel,
                indicators = indicators.distinctBy { it.code },
                explanation = if (indicators.isNotEmpty()) "Social engineering pressure or deceptive links identified in text body." else "No coercive language or deceptive artifacts detected.",
                recommendedAction = if (indicators.isNotEmpty()) "Do not click embedded links, open attachments, or reply with confidential credentials." else "Message text appears benign.",
                analyzedAt = System.currentTimeMillis()
            )
        )
    }
}
