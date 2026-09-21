package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.offline.domain.SyncState

@Entity(tableName = "guardian_devices")
data class GuardianDeviceEntity(
    @PrimaryKey val deviceId: String,
    val userId: String,
    val name: String,
    val macAddress: String,
    val deviceType: String,
    val enrollmentDate: Long,
    val lastHeartbeatAt: Long? = null,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
    val isPaired: Boolean = false,
    val isAuthorized: Boolean = true,
    val syncState: SyncState = SyncState.LOCAL_ONLY
)
