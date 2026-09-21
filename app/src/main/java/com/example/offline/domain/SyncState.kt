package com.example.offline.domain

import kotlinx.coroutines.flow.Flow

enum class SyncState {
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    FAILED,
    CONFLICT
}

interface SyncableRecord {
    val syncState: SyncState
    val lastSyncedAt: Long?
    val localVersion: Int
}

interface OfflineSyncManager {
    suspend fun enqueueForSync(recordType: String, recordId: String)
    suspend fun processPendingSync(): Int
    fun observePendingSyncCount(): Flow<Int>
}
