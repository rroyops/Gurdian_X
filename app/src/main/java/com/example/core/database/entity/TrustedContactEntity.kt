package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.offline.domain.SyncState

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey val contactId: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val relationship: String,
    val isEmergencyRecipient: Boolean = true,
    val priorityOrder: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val lastSyncedAt: Long? = null
)
