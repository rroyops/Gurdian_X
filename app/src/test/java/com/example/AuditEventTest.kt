package com.example

import com.example.core.logging.AuditAction
import com.example.core.logging.AuditEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuditEventTest {

    @Test
    fun testIntegrityHashConsistency() {
        val hash1 = AuditEvent.computeIntegrityHash(
            eventId = "evt_001",
            timestamp = 1700000000000L,
            actorId = "usr_alpha",
            action = AuditAction.EMERGENCY_STARTED,
            resourceId = "emg_123"
        )
        val hash2 = AuditEvent.computeIntegrityHash(
            eventId = "evt_001",
            timestamp = 1700000000000L,
            actorId = "usr_alpha",
            action = AuditAction.EMERGENCY_STARTED,
            resourceId = "emg_123"
        )
        assertEquals(hash1, hash2)

        // Any tamper must change the hash
        val tamperedHash = AuditEvent.computeIntegrityHash(
            eventId = "evt_001",
            timestamp = 1700000000001L, // 1ms difference
            actorId = "usr_alpha",
            action = AuditAction.EMERGENCY_STARTED,
            resourceId = "emg_123"
        )
        assertNotEquals(hash1, tamperedHash)
    }
}
