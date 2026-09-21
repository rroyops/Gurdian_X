package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.logging.AuditAction
import com.example.core.logging.AuditSeverity

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val eventId: String,
    val timestamp: Long,
    val actorId: String,
    val action: AuditAction,
    val severity: AuditSeverity,
    val resourceId: String?,
    val details: String,
    val integrityHash: String
)
