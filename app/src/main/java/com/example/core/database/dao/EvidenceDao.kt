package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.EvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Update
    suspend fun updateEvidence(evidence: EvidenceEntity)

    @Query("SELECT * FROM evidence_records WHERE evidenceId = :evidenceId")
    suspend fun getEvidenceById(evidenceId: String): EvidenceEntity?

    @Query("SELECT * FROM evidence_records WHERE sessionId = :sessionId ORDER BY capturedAt DESC")
    suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence_records ORDER BY capturedAt DESC")
    fun observeAllEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT COUNT(*) FROM evidence_records")
    suspend fun getEvidenceCount(): Int

    @Query("DELETE FROM evidence_records WHERE evidenceId = :evidenceId")
    suspend fun deleteEvidence(evidenceId: String)
}
