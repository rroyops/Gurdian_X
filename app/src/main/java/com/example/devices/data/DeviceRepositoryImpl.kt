package com.example.devices.data

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.IdGenerator
import com.example.core.common.TimeProvider
import com.example.core.database.dao.GuardianDeviceDao
import com.example.core.database.entity.GuardianDeviceEntity
import com.example.core.error.AppError
import com.example.core.security.SecuritySanitizer
import com.example.devices.domain.DeviceRepository
import com.example.devices.domain.DeviceType
import com.example.devices.domain.GuardianDevice
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DeviceRepositoryImpl(
    private val deviceDao: GuardianDeviceDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : DeviceRepository {

    override fun observeDevices(userId: String): Flow<List<GuardianDevice>> {
        return deviceDao.observeDevicesForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDevices(userId: String): List<GuardianDevice> = withContext(dispatchers.io) {
        deviceDao.getDevicesForUser(userId).map { it.toDomain() }
    }

    override suspend fun enrollDevice(device: GuardianDevice): AppResult<GuardianDevice> = withContext(dispatchers.io) {
        try {
            if (!SecuritySanitizer.isValidMacAddress(device.macAddress)) {
                return@withContext AppResult.Error(
                    AppError.ValidationError("macAddress", "Invalid Bluetooth MAC address format")
                )
            }

            val deviceId = if (device.deviceId.isNotBlank()) device.deviceId else idGenerator.newId()
            val now = timeProvider.currentTimeMillis()
            val entity = GuardianDeviceEntity(
                deviceId = deviceId,
                userId = device.userId,
                name = SecuritySanitizer.sanitizeText(device.name, maxLength = 60),
                macAddress = device.macAddress.uppercase(),
                deviceType = device.deviceType.name,
                enrollmentDate = if (device.enrollmentDate > 0L) device.enrollmentDate else now,
                lastHeartbeatAt = device.lastHeartbeatAt,
                batteryLevel = device.batteryLevel,
                firmwareVersion = device.firmwareVersion,
                isPaired = device.isPaired,
                isAuthorized = device.isAuthorized,
                syncState = SyncState.PENDING_UPLOAD
            )
            deviceDao.insertDevice(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to enroll device", e))
        }
    }

    override suspend fun updateHeartbeat(deviceId: String, timestamp: Long, battery: Int?): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deviceDao.updateHeartbeat(deviceId, timestamp, battery)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to update heartbeat", e))
        }
    }

    override suspend fun revokeDevice(deviceId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deviceDao.deleteDevice(deviceId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to revoke device", e))
        }
    }

    override suspend fun getDeviceById(deviceId: String): GuardianDevice? = withContext(dispatchers.io) {
        deviceDao.getDeviceById(deviceId)?.toDomain()
    }

    private fun GuardianDeviceEntity.toDomain(): GuardianDevice {
        val type = try {
            DeviceType.valueOf(deviceType)
        } catch (_: Exception) {
            DeviceType.ESP32_BLE_BEACON
        }
        return GuardianDevice(
            deviceId = deviceId,
            userId = userId,
            name = name,
            macAddress = macAddress,
            deviceType = type,
            enrollmentDate = enrollmentDate,
            lastHeartbeatAt = lastHeartbeatAt,
            batteryLevel = batteryLevel,
            firmwareVersion = firmwareVersion,
            isPaired = isPaired,
            isAuthorized = isAuthorized,
            syncState = syncState
        )
    }
}
