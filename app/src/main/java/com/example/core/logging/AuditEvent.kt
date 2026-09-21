package com.example.core.logging

import java.security.MessageDigest

enum class AuditAction {
    LOGIN,
    LOGOUT,
    DEVICE_ENROLLED,
    DEVICE_REVOKED,
    CONTACT_ADDED,
    CONTACT_REMOVED,
    EMERGENCY_STARTED,
    EMERGENCY_RESOLVED,
    EMERGENCY_ESCALATED,
    EVIDENCE_CAPTURED,
    EVIDENCE_UPLOADED,
    EVIDENCE_DELETED,
    SECURITY_SETTING_CHANGED,
    SECURITY_THREAT_DETECTED,
    ALERT_DISPATCHED
}

enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class AuditEvent(
    val eventId: String,
    val timestamp: Long,
    val actorId: String,
    val action: AuditAction,
    val severity: AuditSeverity,
    val resourceId: String? = null,
    val details: String = "",
    val integrityHash: String = ""
) {
    companion object {
        fun computeIntegrityHash(
            eventId: String,
            timestamp: Long,
            actorId: String,
            action: AuditAction,
            resourceId: String?
        ): String {
            val raw = "$eventId:$timestamp:$actorId:${action.name}:${resourceId.orEmpty()}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

interface AuditLogger {
    suspend fun recordEvent(
        actorId: String,
        action: AuditAction,
        severity: AuditSeverity,
        resourceId: String? = null,
        details: String = ""
    ): AuditEvent

    suspend fun getRecentAuditEvents(limit: Int = 50): List<AuditEvent>
}
