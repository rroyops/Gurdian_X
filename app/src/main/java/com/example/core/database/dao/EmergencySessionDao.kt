package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.EmergencySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: EmergencySessionEntity)

    @Update
    suspend fun updateSession(session: EmergencySessionEntity)

    @Query("SELECT * FROM emergency_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): EmergencySessionEntity?

    @Query("SELECT * FROM emergency_sessions WHERE status IN ('ARMED', 'TRIGGERED', 'ACTIVE', 'ESCALATING') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): EmergencySessionEntity?

    @Query("SELECT * FROM emergency_sessions WHERE status IN ('ARMED', 'TRIGGERED', 'ACTIVE', 'ESCALATING') ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<EmergencySessionEntity?>

    @Query("SELECT * FROM emergency_sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<EmergencySessionEntity>>

    @Query("SELECT * FROM emergency_sessions WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingSyncSessions(): List<EmergencySessionEntity>
}
