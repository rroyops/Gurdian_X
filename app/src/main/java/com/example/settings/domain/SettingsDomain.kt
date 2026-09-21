package com.example.settings.domain

import com.example.core.common.AppResult
import kotlinx.coroutines.flow.Flow

data class GuardianSettings(
    val userId: String,
    val isDarkTheme: Boolean = true,
    val sosCountdownSeconds: Int = 5,
    val isStealthModeEnabled: Boolean = false,
    val bleHeartbeatIntervalSeconds: Int = 10,
    val autoEscalateMinutes: Int = 5,
    val isEvidenceAutoCaptureEnabled: Boolean = false,
    val isHighAccuracyLocationEnabled: Boolean = true,
    val isContinuousMonitoringEnabled: Boolean = true
)

interface SettingsRepository {
    suspend fun getSettings(userId: String): GuardianSettings
    fun observeSettings(userId: String): Flow<GuardianSettings>
    suspend fun updateSettings(settings: GuardianSettings): AppResult<GuardianSettings>
}
