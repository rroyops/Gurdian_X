package com.example.core.di

import android.content.Context
import com.example.contacts.data.ContactRepositoryImpl
import com.example.contacts.domain.ContactRepository
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.common.IdGenerator
import com.example.core.common.SystemTimeProvider
import com.example.core.common.TimeProvider
import com.example.core.common.UuidGenerator
import com.example.core.database.GuardianDatabase
import com.example.core.hardware.AndroidHapticFeedbackController
import com.example.core.hardware.HapticFeedbackController
import com.example.core.logging.AndroidGuardianLogger
import com.example.core.logging.AuditLogger
import com.example.core.logging.AuditLoggerImpl
import com.example.core.logging.GuardianLogger
import com.example.core.network.AndroidNetworkStatusObserver
import com.example.core.network.NetworkStatusObserver
import com.example.devices.data.DeviceRepositoryImpl
import com.example.devices.domain.DeviceRepository
import com.example.devices.domain.HardwareContinuityMonitor
import com.example.devices.domain.SystemHardwareContinuityMonitor
import com.example.emergency.data.EmergencyRepositoryImpl
import com.example.emergency.domain.EmergencyRepository
import com.example.evidence.data.EvidenceRepositoryImpl
import com.example.notifications.domain.EmergencyNotificationDispatcher
import com.example.offline.data.OfflineSyncManagerImpl
import com.example.offline.domain.OfflineSyncManager

object ServiceLocator {
    private var applicationContext: Context? = null

    val logger: GuardianLogger by lazy { AndroidGuardianLogger() }
    val dispatchers: DispatcherProvider by lazy { DefaultDispatcherProvider() }
    val timeProvider: TimeProvider by lazy { SystemTimeProvider() }
    val idGenerator: IdGenerator by lazy { UuidGenerator() }

    val networkStatusObserver: NetworkStatusObserver by lazy {
        val context = checkNotNull(applicationContext) { "ServiceLocator must be initialized with Context" }
        AndroidNetworkStatusObserver(context)
    }

    val database: GuardianDatabase by lazy {
        val context = checkNotNull(applicationContext) { "ServiceLocator must be initialized with Context" }
        GuardianDatabase.getInstance(context)
    }

    val auditLogger: AuditLogger by lazy {
        AuditLoggerImpl(
            auditDao = database.auditEventDao(),
            idGenerator = idGenerator,
            timeProvider = timeProvider,
            dispatchers = dispatchers
        )
    }

    val emergencyRepository: EmergencyRepository by lazy {
        EmergencyRepositoryImpl(
            sessionDao = database.emergencySessionDao(),
            idGenerator = idGenerator,
            timeProvider = timeProvider,
            dispatchers = dispatchers
        )
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepositoryImpl(
            contactDao = database.trustedContactDao(),
            idGenerator = idGenerator,
            timeProvider = timeProvider,
            dispatchers = dispatchers
        )
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepositoryImpl(
            deviceDao = database.guardianDeviceDao(),
            idGenerator = idGenerator,
            timeProvider = timeProvider,
            dispatchers = dispatchers
        )
    }

    val evidenceRepository: EvidenceRepositoryImpl by lazy {
        EvidenceRepositoryImpl(
            evidenceDao = database.evidenceDao(),
            dispatchers = dispatchers
        )
    }

    val offlineSyncManager: OfflineSyncManager by lazy {
        OfflineSyncManagerImpl(
            database = database,
            dispatchers = dispatchers
        )
    }

    val hapticController: HapticFeedbackController by lazy {
        val context = checkNotNull(applicationContext) { "ServiceLocator must be initialized with Context" }
        AndroidHapticFeedbackController(context)
    }

    val emailSender: com.example.notifications.email.EmailSender by lazy {
        val smtp = com.example.notifications.email.SecureSmtpEmailSender()
        val rest = com.example.notifications.email.RestEmailSender(
            senderEmailProvider = { com.example.notifications.email.EmailConfigProvider.getSenderEmail() },
            apiKeyProvider = { com.example.notifications.email.EmailConfigProvider.getApiKey() }
        )
        com.example.notifications.email.CompositeEmergencyEmailSender(
            smtpSender = smtp,
            restSender = rest
        )
    }

    val notificationDispatcher: EmergencyNotificationDispatcher by lazy {
        val context = checkNotNull(applicationContext) { "ServiceLocator must be initialized with Context" }
        com.example.notifications.data.RealEmergencyNotificationDispatcher(
            context = context,
            emailSender = emailSender
        )
    }

    val hardwareMonitor: HardwareContinuityMonitor by lazy {
        SystemHardwareContinuityMonitor(deviceRepository)
    }

    val locationProvider: com.example.location.domain.LocationProvider by lazy {
        val context = checkNotNull(applicationContext) { "ServiceLocator must be initialized with Context" }
        com.example.location.AndroidLocationProvider(context)
    }

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            logger.i("ServiceLocator", "GuardianX ServiceLocator initialized successfully")
        }
    }
}
