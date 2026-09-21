package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContactEntity)

    @Update
    suspend fun updateContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE contactId = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("SELECT * FROM trusted_contacts WHERE contactId = :contactId LIMIT 1")
    suspend fun getContactById(contactId: String): TrustedContactEntity?

    @Query("SELECT * FROM trusted_contacts WHERE userId = :userId ORDER BY priorityOrder ASC, name ASC")
    fun observeContactsForUser(userId: String): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts WHERE userId = :userId ORDER BY priorityOrder ASC, name ASC")
    suspend fun getContactsForUser(userId: String): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun getPendingSyncContacts(): List<TrustedContactEntity>
}
