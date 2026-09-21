package com.example.core.database

import androidx.room.TypeConverter
import com.example.core.logging.AuditAction
import com.example.core.logging.AuditSeverity
import com.example.offline.domain.SyncState

class Converters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = try {
        SyncState.valueOf(value)
    } catch (_: Exception) {
        SyncState.LOCAL_ONLY
    }

    @TypeConverter
    fun fromAuditAction(value: AuditAction): String = value.name

    @TypeConverter
    fun toAuditAction(value: String): AuditAction = try {
        AuditAction.valueOf(value)
    } catch (_: Exception) {
        AuditAction.SECURITY_THREAT_DETECTED
    }

    @TypeConverter
    fun fromAuditSeverity(value: AuditSeverity): String = value.name

    @TypeConverter
    fun toAuditSeverity(value: String): AuditSeverity = try {
        AuditSeverity.valueOf(value)
    } catch (_: Exception) {
        AuditSeverity.INFO
    }
}
