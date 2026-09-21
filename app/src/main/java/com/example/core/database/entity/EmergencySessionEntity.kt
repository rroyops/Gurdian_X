package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.offline.domain.SyncState

@Entity(tableName = "emergency_sessions")
data class EmergencySessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val triggerType: String,
    val status: String,
    val startedAt: Long,
    val resolvedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val notes: String = "",
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val lastSyncedAt: Long? = null,
    val localVersion: Int = 1
)
