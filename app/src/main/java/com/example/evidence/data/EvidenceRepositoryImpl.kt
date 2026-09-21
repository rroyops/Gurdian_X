package com.example.evidence.data

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.database.dao.EvidenceDao
import com.example.core.database.entity.EvidenceEntity
import com.example.core.error.AppError
import com.example.evidence.domain.EncryptionStatus
import com.example.evidence.domain.EvidenceRecord
import com.example.evidence.domain.EvidenceRepository
import com.example.evidence.domain.EvidenceType
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao,
    private val dispatchers: DispatcherProvider
) : EvidenceRepository {

    override suspend fun createEvidenceRecord(record: EvidenceRecord): AppResult<EvidenceRecord> = withContext(dispatchers.io) {
        try {
            val entity = record.toEntity()
            evidenceDao.insertEvidence(entity)
            AppResult.Success(record)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to store evidence record: ${e.message}", e))
        }
    }

    override suspend fun getEvidenceForSession(sessionId: String): List<EvidenceRecord> = withContext(dispatchers.io) {
        evidenceDao.getEvidenceForSession(sessionId).map { it.toDomain() }
    }

    override suspend fun verifyIntegrity(evidenceId: String, currentFileChecksum: String): Boolean = withContext(dispatchers.io) {
        val entity = evidenceDao.getEvidenceById(evidenceId) ?: return@withContext false
        entity.sha256Checksum.equals(currentFileChecksum, ignoreCase = true)
    }

    override suspend fun markUploaded(evidenceId: String, remoteUri: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val entity = evidenceDao.getEvidenceById(evidenceId)
                ?: return@withContext AppResult.Error(AppError.DatabaseError("Evidence record not found"))
            val updated = entity.copy(
                remoteStorageUri = remoteUri,
                isUploadVerified = true,
                syncState = SyncState.SYNCED
            )
            evidenceDao.updateEvidence(updated)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to mark evidence uploaded: ${e.message}", e))
        }
    }

    override suspend fun deleteEvidenceRecord(evidenceId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            evidenceDao.deleteEvidence(evidenceId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to delete evidence record", e))
        }
    }

    fun observeAllEvidence(): Flow<List<EvidenceRecord>> {
        return evidenceDao.observeAllEvidence().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getEvidenceCount(): Int = withContext(dispatchers.io) {
        evidenceDao.getEvidenceCount()
    }

    private fun EvidenceRecord.toEntity(): EvidenceEntity = EvidenceEntity(
        evidenceId = evidenceId,
        sessionId = sessionId,
        type = type.name,
        localFilePath = localFilePath,
        remoteStorageUri = remoteStorageUri,
        sha256Checksum = sha256Checksum,
        fileSizeBytes = fileSizeBytes,
        capturedAt = capturedAt,
        encryptionStatus = encryptionStatus.name,
        syncState = syncState,
        isUploadVerified = isUploadVerified
    )

    private fun EvidenceEntity.toDomain(): EvidenceRecord {
        val evType = try { EvidenceType.valueOf(type) } catch (_: Exception) { EvidenceType.INCIDENT_NOTES }
        val encStatus = try { EncryptionStatus.valueOf(encryptionStatus) } catch (_: Exception) { EncryptionStatus.UNENCRYPTED }
        return EvidenceRecord(
            evidenceId = evidenceId,
            sessionId = sessionId,
            type = evType,
            localFilePath = localFilePath,
            remoteStorageUri = remoteStorageUri,
            sha256Checksum = sha256Checksum,
            fileSizeBytes = fileSizeBytes,
            capturedAt = capturedAt,
            encryptionStatus = encStatus,
            syncState = syncState,
            isUploadVerified = isUploadVerified
        )
    }
}
