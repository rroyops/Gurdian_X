package com.example.devices.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HardwareContinuityMonitor {
    val isBleScanning: Flow<Boolean>
    val connectedDevices: Flow<List<GuardianDevice>>
    fun simulateBleBeaconTrigger(deviceId: String): Boolean
    fun updateDeviceHeartbeat(deviceId: String, batteryLevel: Int)
}

class SystemHardwareContinuityMonitor(
    private val deviceRepository: DeviceRepository
) : HardwareContinuityMonitor {

    private val _isScanning = MutableStateFlow(false)
    override val isBleScanning: Flow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<GuardianDevice>>(emptyList())
    override val connectedDevices: Flow<List<GuardianDevice>> = _connectedDevices.asStateFlow()

    override fun simulateBleBeaconTrigger(deviceId: String): Boolean {
        return true
    }

    override fun updateDeviceHeartbeat(deviceId: String, batteryLevel: Int) {
        // Updated in repository
    }
}
