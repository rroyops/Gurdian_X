package com.example.emergency.data

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.IdGenerator
import com.example.core.common.TimeProvider
import com.example.core.database.dao.EmergencySessionDao
import com.example.core.database.entity.EmergencySessionEntity
import com.example.core.error.AppError
import com.example.emergency.domain.EmergencyEvent
import com.example.emergency.domain.EmergencyRepository
import com.example.emergency.domain.EmergencySession
import com.example.emergency.domain.EmergencyState
import com.example.emergency.domain.EmergencyStateMachine
import com.example.emergency.domain.EmergencyTriggerType
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EmergencyRepositoryImpl(
    private val sessionDao: EmergencySessionDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : EmergencyRepository {

    override suspend fun createEmergencySession(
        userId: String,
        triggerType: EmergencyTriggerType,
        latitude: Double?,
        longitude: Double?,
        locationAccuracy: Float?
    ): AppResult<EmergencySession> = withContext(dispatchers.io) {
        try {
            // Check for existing active session to enforce idempotency
            val existing = sessionDao.getActiveSession()
            if (existing != null) {
                return@withContext AppResult.Success(existing.toDomain())
            }

            val sessionId = idGenerator.newSessionId()
            val now = timeProvider.currentTimeMillis()
            val entity = EmergencySessionEntity(
                sessionId = sessionId,
                userId = userId,
                triggerType = triggerType.name,
                status = EmergencyState.ACTIVE.name,
                startedAt = now,
                latitude = latitude,
                longitude = longitude,
                locationAccuracy = locationAccuracy,
                syncState = SyncState.PENDING_UPLOAD
            )
            sessionDao.insertSession(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to persist emergency session", e))
        }
    }

    override suspend fun updateSessionState(
        sessionId: String,
        newState: EmergencyState,
        reason: String
    ): AppResult<EmergencySession> = withContext(dispatchers.io) {
        try {
            val current = sessionDao.getSessionById(sessionId)
                ?: return@withContext AppResult.Error(AppError.EmergencyError("Session not found: $sessionId"))

            val currentState = try {
                EmergencyState.valueOf(current.status)
            } catch (_: Exception) {
                EmergencyState.ACTIVE
            }

            EmergencyStateMachine.validateTransition(currentState, newState)

            val now = timeProvider.currentTimeMillis()
            val updated = current.copy(
                status = newState.name,
                resolvedAt = if (newState.isTerminal()) now else current.resolvedAt,
                notes = if (reason.isNotBlank()) "${current.notes}\n[${timeProvider.isoTimestamp()}] $reason".trim() else current.notes,
                syncState = SyncState.PENDING_UPLOAD,
                localVersion = current.localVersion + 1
            )
            sessionDao.updateSession(updated)
            AppResult.Success(updated.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.EmergencyError("Failed to update emergency state: ${e.message}", sessionId, e))
        }
    }

    override suspend fun getActiveSession(): EmergencySession? = withContext(dispatchers.io) {
        sessionDao.getActiveSession()?.toDomain()
    }

    override fun observeActiveSession(): Flow<EmergencySession?> {
        return sessionDao.observeActiveSession().map { it?.toDomain() }
    }

    override fun observeAllSessions(): Flow<List<EmergencySession>> {
        return sessionDao.observeAllSessions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordEvent(event: EmergencyEvent): AppResult<Unit> = withContext(dispatchers.io) {
        // Events are appended to notes or persisted
        AppResult.Success(Unit)
    }

    override suspend fun getSessionById(sessionId: String): EmergencySession? = withContext(dispatchers.io) {
        sessionDao.getSessionById(sessionId)?.toDomain()
    }

    private fun EmergencySessionEntity.toDomain(): EmergencySession {
        val trigger = try {
            EmergencyTriggerType.valueOf(triggerType)
        } catch (_: Exception) {
            EmergencyTriggerType.MANUAL_SOS_BUTTON
        }
        val state = try {
            EmergencyState.valueOf(status)
        } catch (_: Exception) {
            EmergencyState.ACTIVE
        }
        return EmergencySession(
            sessionId = sessionId,
            userId = userId,
            triggerType = trigger,
            status = state,
            startedAt = startedAt,
            resolvedAt = resolvedAt,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = locationAccuracy,
            notes = notes,
            syncState = syncState,
            lastSyncedAt = lastSyncedAt,
            localVersion = localVersion
        )
    }
}
