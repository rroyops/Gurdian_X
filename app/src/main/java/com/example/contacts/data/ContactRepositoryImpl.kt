package com.example.contacts.data

import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.ContactRepository
import com.example.contacts.domain.TrustedContact
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.IdGenerator
import com.example.core.common.TimeProvider
import com.example.core.database.dao.TrustedContactDao
import com.example.core.database.entity.TrustedContactEntity
import com.example.core.error.AppError
import com.example.core.security.SecuritySanitizer
import com.example.offline.domain.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ContactRepositoryImpl(
    private val contactDao: TrustedContactDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ContactRepository {

    override fun observeContacts(userId: String): Flow<List<TrustedContact>> {
        return contactDao.observeContactsForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getContacts(userId: String): List<TrustedContact> = withContext(dispatchers.io) {
        contactDao.getContactsForUser(userId).map { it.toDomain() }
    }

    override suspend fun addContact(contact: TrustedContact): AppResult<TrustedContact> = withContext(dispatchers.io) {
        try {
            val sanitizedPhone = SecuritySanitizer.sanitizePhoneNumber(contact.phoneNumber)
            if (!SecuritySanitizer.isValidPhoneNumber(sanitizedPhone)) {
                return@withContext AppResult.Error(
                    AppError.ValidationError("phoneNumber", "Phone number must be between 7 and 15 valid digits")
                )
            }
            if (contact.email.isNotBlank() && !SecuritySanitizer.isValidEmail(contact.email)) {
                return@withContext AppResult.Error(
                    AppError.ValidationError("email", "Invalid email address format")
                )
            }

            val contactId = if (contact.contactId.isNotBlank()) contact.contactId else idGenerator.newId()
            val now = timeProvider.currentTimeMillis()
            val entity = TrustedContactEntity(
                contactId = contactId,
                userId = contact.userId,
                name = SecuritySanitizer.sanitizeText(contact.name, maxLength = 80),
                phoneNumber = sanitizedPhone,
                email = contact.email.trim(),
                relationship = contact.relationship.name,
                isEmergencyRecipient = contact.isEmergencyRecipient,
                priorityOrder = contact.priorityOrder,
                createdAt = if (contact.createdAt > 0L) contact.createdAt else now,
                updatedAt = now,
                syncState = SyncState.PENDING_UPLOAD
            )
            contactDao.insertContact(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to add contact", e))
        }
    }

    override suspend fun updateContact(contact: TrustedContact): AppResult<TrustedContact> = withContext(dispatchers.io) {
        try {
            val sanitizedPhone = SecuritySanitizer.sanitizePhoneNumber(contact.phoneNumber)
            if (!SecuritySanitizer.isValidPhoneNumber(sanitizedPhone)) {
                return@withContext AppResult.Error(
                    AppError.ValidationError("phoneNumber", "Invalid phone number format")
                )
            }
            val now = timeProvider.currentTimeMillis()
            val entity = TrustedContactEntity(
                contactId = contact.contactId,
                userId = contact.userId,
                name = SecuritySanitizer.sanitizeText(contact.name, maxLength = 80),
                phoneNumber = sanitizedPhone,
                email = contact.email.trim(),
                relationship = contact.relationship.name,
                isEmergencyRecipient = contact.isEmergencyRecipient,
                priorityOrder = contact.priorityOrder,
                createdAt = contact.createdAt,
                updatedAt = now,
                syncState = SyncState.PENDING_UPLOAD
            )
            contactDao.updateContact(entity)
            AppResult.Success(entity.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to update contact", e))
        }
    }

    override suspend fun deleteContact(contactId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            contactDao.deleteContact(contactId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Failed to delete contact", e))
        }
    }

    override suspend fun getContactById(contactId: String): TrustedContact? = withContext(dispatchers.io) {
        contactDao.getContactById(contactId)?.toDomain()
    }

    private fun TrustedContactEntity.toDomain(): TrustedContact {
        val rel = try {
            ContactRelationship.valueOf(relationship)
        } catch (_: Exception) {
            ContactRelationship.OTHER
        }
        return TrustedContact(
            contactId = contactId,
            userId = userId,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            relationship = rel,
            isEmergencyRecipient = isEmergencyRecipient,
            priorityOrder = priorityOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncState = syncState
        )
    }
}
