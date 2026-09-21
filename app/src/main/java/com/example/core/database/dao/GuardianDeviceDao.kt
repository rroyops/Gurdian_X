package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.GuardianDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardianDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: GuardianDeviceEntity)

    @Update
    suspend fun updateDevice(device: GuardianDeviceEntity)

    @Query("DELETE FROM guardian_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    @Query("SELECT * FROM guardian_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): GuardianDeviceEntity?

    @Query("SELECT * FROM guardian_devices WHERE userId = :userId ORDER BY enrollmentDate DESC")
    fun observeDevicesForUser(userId: String): Flow<List<GuardianDeviceEntity>>

    @Query("SELECT * FROM guardian_devices WHERE userId = :userId ORDER BY enrollmentDate DESC")
    suspend fun getDevicesForUser(userId: String): List<GuardianDeviceEntity>

    @Query("UPDATE guardian_devices SET lastHeartbeatAt = :timestamp, batteryLevel = :battery WHERE deviceId = :deviceId")
    suspend fun updateHeartbeat(deviceId: String, timestamp: Long, battery: Int?)
}
