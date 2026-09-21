package com.example

import com.example.evidence.domain.EvidenceChecksumCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EvidenceIntegrityTest {

    @Test
    fun testEvidenceSha256ChecksumIntegrity() {
        val payload1 = "GPS_TELEMETRY: LAT=37.7749 LNG=-122.4194 TIMESTAMP=1700000000"
        val payload2 = "GPS_TELEMETRY: LAT=37.7749 LNG=-122.4194 TIMESTAMP=1700000000"
        val tamperedPayload = "GPS_TELEMETRY: LAT=37.7749 LNG=-122.4194 TIMESTAMP=1700000001"

        val hash1 = EvidenceChecksumCalculator.calculateSha256ForText(payload1)
        val hash2 = EvidenceChecksumCalculator.calculateSha256ForText(payload2)
        val tamperedHash = EvidenceChecksumCalculator.calculateSha256ForText(tamperedPayload)

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, tamperedHash)
    }
}
