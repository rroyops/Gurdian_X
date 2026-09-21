package com.example.devices.domain

import com.example.core.common.AppResult
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow

enum class DeviceType {
    ESP32_BLE_BEACON,
    SMARTWATCH,
    EMERGENCY_WRISTBAND,
    GSM_MODULE,
    OTHER
}

enum class BleStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    HEARTBEAT_ACTIVE,
    LINK_LOST,
    ERROR
}

data class GuardianDevice(
    val deviceId: String,
    val userId: String,
    val name: String,
    val macAddress: String,
    val deviceType: DeviceType,
    val enrollmentDate: Long,
    val lastHeartbeatAt: Long? = null,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
    val isPaired: Boolean = false,
    val isAuthorized: Boolean = true,
    val syncState: SyncState = SyncState.LOCAL_ONLY
)

interface DeviceRepository {
    fun observeDevices(userId: String): Flow<List<GuardianDevice>>
    suspend fun getDevices(userId: String): List<GuardianDevice>
    suspend fun enrollDevice(device: GuardianDevice): AppResult<GuardianDevice>
    suspend fun updateHeartbeat(deviceId: String, timestamp: Long, battery: Int?): AppResult<Unit>
    suspend fun revokeDevice(deviceId: String): AppResult<Unit>
    suspend fun getDeviceById(deviceId: String): GuardianDevice?
}
