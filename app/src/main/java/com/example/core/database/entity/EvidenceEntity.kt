package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.offline.domain.SyncState

@Entity(tableName = "evidence_records")
data class EvidenceEntity(
    @PrimaryKey
    val evidenceId: String,
    val sessionId: String,
    val type: String,
    val localFilePath: String,
    val remoteStorageUri: String?,
    val sha256Checksum: String,
    val fileSizeBytes: Long,
    val capturedAt: Long,
    val encryptionStatus: String,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val isUploadVerified: Boolean = false
)
