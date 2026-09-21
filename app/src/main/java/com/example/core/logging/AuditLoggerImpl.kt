package com.example.core.logging

import com.example.core.common.DispatcherProvider
import com.example.core.common.IdGenerator
import com.example.core.common.TimeProvider
import com.example.core.database.dao.AuditEventDao
import com.example.core.database.entity.AuditEventEntity
import kotlinx.coroutines.withContext

class AuditLoggerImpl(
    private val auditDao: AuditEventDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : AuditLogger {

    override suspend fun recordEvent(
        actorId: String,
        action: AuditAction,
        severity: AuditSeverity,
        resourceId: String?,
        details: String
    ): AuditEvent = withContext(dispatchers.io) {
        val eventId = idGenerator.newId()
        val timestamp = timeProvider.currentTimeMillis()
        val hash = AuditEvent.computeIntegrityHash(
            eventId = eventId,
            timestamp = timestamp,
            actorId = actorId,
            action = action,
            resourceId = resourceId
        )
        val entity = AuditEventEntity(
            eventId = eventId,
            timestamp = timestamp,
            actorId = actorId,
            action = action,
            severity = severity,
            resourceId = resourceId,
            details = details,
            integrityHash = hash
        )
        auditDao.insertEvent(entity)

        AuditEvent(
            eventId = eventId,
            timestamp = timestamp,
            actorId = actorId,
            action = action,
            severity = severity,
            resourceId = resourceId,
            details = details,
            integrityHash = hash
        )
    }

    override suspend fun getRecentAuditEvents(limit: Int): List<AuditEvent> = withContext(dispatchers.io) {
        auditDao.getRecentEvents(limit).map { entity ->
            AuditEvent(
                eventId = entity.eventId,
                timestamp = entity.timestamp,
                actorId = entity.actorId,
                action = entity.action,
                severity = entity.severity,
                resourceId = entity.resourceId,
                details = entity.details,
                integrityHash = entity.integrityHash
            )
        }
    }
}
