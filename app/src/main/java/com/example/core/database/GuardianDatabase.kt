package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.core.database.dao.AuditEventDao
import com.example.core.database.dao.EmergencySessionDao
import com.example.core.database.dao.EvidenceDao
import com.example.core.database.dao.GuardianDeviceDao
import com.example.core.database.dao.TrustedContactDao
import com.example.core.database.entity.AuditEventEntity
import com.example.core.database.entity.EmergencySessionEntity
import com.example.core.database.entity.EvidenceEntity
import com.example.core.database.entity.GuardianDeviceEntity
import com.example.core.database.entity.TrustedContactEntity

@Database(
    entities = [
        EmergencySessionEntity::class,
        TrustedContactEntity::class,
        GuardianDeviceEntity::class,
        AuditEventEntity::class,
        EvidenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun emergencySessionDao(): EmergencySessionDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun guardianDeviceDao(): GuardianDeviceDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        private const val DATABASE_NAME = "guardianx_secure.db"

        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getInstance(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
