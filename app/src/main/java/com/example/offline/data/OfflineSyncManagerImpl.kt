package com.example.offline.data

import com.example.core.common.DispatcherProvider
import com.example.core.database.GuardianDatabase
import com.example.offline.domain.OfflineSyncManager
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class OfflineSyncManagerImpl(
    private val database: GuardianDatabase,
    private val dispatchers: DispatcherProvider
) : OfflineSyncManager {

    private val _pendingSyncCount = MutableStateFlow(0)
    override fun observePendingSyncCount(): Flow<Int> = _pendingSyncCount.asStateFlow()

    override suspend fun enqueueForSync(recordType: String, recordId: String) = withContext(dispatchers.io) {
        refreshPendingCount()
    }

    override suspend fun processPendingSync(): Int = withContext(dispatchers.io) {
        val pendingSessions = database.emergencySessionDao().getPendingSyncSessions()
        var syncedCount = 0

        for (session in pendingSessions) {
            val updated = session.copy(
                syncState = SyncState.SYNCED,
                lastSyncedAt = System.currentTimeMillis()
            )
            database.emergencySessionDao().updateSession(updated)
            syncedCount++
        }

        refreshPendingCount()
        syncedCount
    }

    suspend fun refreshPendingCount() = withContext(dispatchers.io) {
        val pendingCount = database.emergencySessionDao().getPendingSyncSessions().size
        _pendingSyncCount.value = pendingCount
    }
}
