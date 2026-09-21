package com.example.emergency.domain

import com.example.core.common.AppResult
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow

enum class EmergencyTriggerType {
    MANUAL_SOS_BUTTON,
    HARDWARE_BLE_BUTTON,
    FALL_DETECTION_SENSOR,
    HEARTBEAT_LOSS,
    VOICE_DURESS,
    TEST_EXERCISE
}

enum class EmergencyState {
    IDLE,
    ARMED,
    TRIGGERED,
    ACTIVE,
    ESCALATING,
    RESOLVED,
    CANCELLED,
    FAILED,
    EXPIRED;

    fun isTerminal(): Boolean = this in listOf(RESOLVED, CANCELLED, FAILED, EXPIRED)
    fun isOngoing(): Boolean = this in listOf(TRIGGERED, ACTIVE, ESCALATING)
}

data class EmergencyEvent(
    val eventId: String,
    val sessionId: String,
    val state: EmergencyState,
    val timestamp: Long,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class EmergencySession(
    val sessionId: String,
    val userId: String,
    val triggerType: EmergencyTriggerType,
    val status: EmergencyState,
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

object EmergencyStateMachine {
    private val VALID_TRANSITIONS = mapOf(
        EmergencyState.IDLE to setOf(EmergencyState.ARMED, EmergencyState.TRIGGERED),
        EmergencyState.ARMED to setOf(EmergencyState.TRIGGERED, EmergencyState.IDLE, EmergencyState.CANCELLED),
        EmergencyState.TRIGGERED to setOf(EmergencyState.ACTIVE, EmergencyState.CANCELLED, EmergencyState.FAILED),
        EmergencyState.ACTIVE to setOf(EmergencyState.ESCALATING, EmergencyState.RESOLVED, EmergencyState.CANCELLED, EmergencyState.FAILED),
        EmergencyState.ESCALATING to setOf(EmergencyState.RESOLVED, EmergencyState.FAILED, EmergencyState.EXPIRED),
        EmergencyState.RESOLVED to emptySet(),
        EmergencyState.CANCELLED to emptySet(),
        EmergencyState.FAILED to emptySet(),
        EmergencyState.EXPIRED to emptySet()
    )

    fun canTransition(from: EmergencyState, to: EmergencyState): Boolean {
        if (from == to) return true
        return VALID_TRANSITIONS[from]?.contains(to) == true
    }

    fun validateTransition(from: EmergencyState, to: EmergencyState) {
        if (!canTransition(from, to)) {
            throw IllegalStateException("Invalid emergency state transition from $from to $to")
        }
    }
}

interface EmergencyRepository {
    suspend fun createEmergencySession(
        userId: String,
        triggerType: EmergencyTriggerType,
        latitude: Double? = null,
        longitude: Double? = null,
        locationAccuracy: Float? = null
    ): AppResult<EmergencySession>

    suspend fun updateSessionState(
        sessionId: String,
        newState: EmergencyState,
        reason: String
    ): AppResult<EmergencySession>

    suspend fun getActiveSession(): EmergencySession?
    fun observeActiveSession(): Flow<EmergencySession?>
    fun observeAllSessions(): Flow<List<EmergencySession>>
    suspend fun recordEvent(event: EmergencyEvent): AppResult<Unit>
    suspend fun getSessionById(sessionId: String): EmergencySession?
}
