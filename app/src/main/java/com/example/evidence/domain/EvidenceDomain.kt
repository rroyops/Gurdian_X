package com.example.evidence.domain

import com.example.core.common.AppResult
import com.example.offline.domain.SyncState

enum class EvidenceType {
    AUDIO_RECORDING,
    PHOTO_CAPTURE,
    SENSOR_TELEMETRY,
    LOCATION_TRAIL,
    INCIDENT_NOTES
}

enum class EncryptionStatus {
    UNENCRYPTED,
    ENCRYPTED_AES_GCM,
    FAILED
}

data class EvidenceRecord(
    val evidenceId: String,
    val sessionId: String,
    val type: EvidenceType,
    val localFilePath: String,
    val remoteStorageUri: String? = null,
    val sha256Checksum: String,
    val fileSizeBytes: Long,
    val capturedAt: Long,
    val encryptionStatus: EncryptionStatus = EncryptionStatus.UNENCRYPTED,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val isUploadVerified: Boolean = false
)

interface EvidenceRepository {
    suspend fun createEvidenceRecord(record: EvidenceRecord): AppResult<EvidenceRecord>
    suspend fun getEvidenceForSession(sessionId: String): List<EvidenceRecord>
    suspend fun verifyIntegrity(evidenceId: String, currentFileChecksum: String): Boolean
    suspend fun markUploaded(evidenceId: String, remoteUri: String): AppResult<Unit>
    suspend fun deleteEvidenceRecord(evidenceId: String): AppResult<Unit>
}
