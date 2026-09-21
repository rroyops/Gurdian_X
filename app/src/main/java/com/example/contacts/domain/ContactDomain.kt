package com.example.contacts.domain

import com.example.core.common.AppResult
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow

enum class ContactRelationship {
    FAMILY,
    FRIEND,
    COLLEAGUE,
    SPOUSE,
    PARENT,
    CHILD,
    DOCTOR,
    SECURITY_TEAM,
    OTHER
}

data class TrustedContact(
    val contactId: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val relationship: ContactRelationship,
    val isEmergencyRecipient: Boolean = true,
    val priorityOrder: Int = 1,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncState: SyncState = SyncState.LOCAL_ONLY
)

interface ContactRepository {
    fun observeContacts(userId: String): Flow<List<TrustedContact>>
    suspend fun getContacts(userId: String): List<TrustedContact>
    suspend fun addContact(contact: TrustedContact): AppResult<TrustedContact>
    suspend fun updateContact(contact: TrustedContact): AppResult<TrustedContact>
    suspend fun deleteContact(contactId: String): AppResult<Unit>
    suspend fun getContactById(contactId: String): TrustedContact?
}
