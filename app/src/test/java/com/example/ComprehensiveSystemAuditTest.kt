package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.TrustedContact
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.SystemTimeProvider
import com.example.core.common.UuidGenerator
import com.example.core.database.GuardianDatabase
import com.example.core.database.entity.TrustedContactEntity
import com.example.devices.data.DeviceRepositoryImpl
import com.example.devices.domain.DeviceType
import com.example.devices.domain.GuardianDevice
import com.example.emergency.data.EmergencyRepositoryImpl
import com.example.emergency.domain.EmergencyState
import com.example.emergency.domain.EmergencyTriggerType
import com.example.offline.data.OfflineSyncManagerImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComprehensiveSystemAuditTest {

    private lateinit var database: GuardianDatabase
    private lateinit var emergencyRepo: EmergencyRepositoryImpl
    private lateinit var deviceRepo: DeviceRepositoryImpl
    private lateinit var syncManager: OfflineSyncManagerImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, GuardianDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val idGen = UuidGenerator()
        val timeProv = SystemTimeProvider()
        val dispatchers = DefaultDispatcherProvider()

        emergencyRepo = EmergencyRepositoryImpl(
            sessionDao = database.emergencySessionDao(),
            idGenerator = idGen,
            timeProvider = timeProv,
            dispatchers = dispatchers
        )

        deviceRepo = DeviceRepositoryImpl(
            deviceDao = database.guardianDeviceDao(),
            idGenerator = idGen,
            timeProvider = timeProv,
            dispatchers = dispatchers
        )

        syncManager = OfflineSyncManagerImpl(
            database = database,
            dispatchers = dispatchers
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // TC-AUTH-01 / TC-AUTH-02: Auth Domain Session Verification
    @Test
    fun testUserSessionCreationAndState() {
        val session = com.example.auth.domain.UserSession(
            uid = "USR_LOCAL_TEST",
            email = "commander@guardianx.safety",
            displayName = "Security Lead",
            phoneNumber = "+14155551234",
            isEmailVerified = true,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        val authState: com.example.auth.domain.AuthState = com.example.auth.domain.AuthState.Authenticated(session)
        assertTrue("AuthState should be Authenticated", authState is com.example.auth.domain.AuthState.Authenticated)
        assertEquals("commander@guardianx.safety", (authState as com.example.auth.domain.AuthState.Authenticated).session.email)
    }

    // TC-EMG-01 / TC-EMG-02: SOS Activation, Idempotency and Dynamic GPS Integrity
    @Test
    fun testEmergencyActivationAndIdempotency() = runBlocking {
        val res1 = emergencyRepo.createEmergencySession(
            userId = "USER_1",
            triggerType = EmergencyTriggerType.MANUAL_SOS_BUTTON,
            latitude = 37.77492,
            longitude = -122.41942,
            locationAccuracy = 4.0f
        )
        assertTrue("First SOS activation must succeed", res1 is com.example.core.common.AppResult.Success)
        val session1 = (res1 as com.example.core.common.AppResult.Success).data
        assertEquals(EmergencyState.ACTIVE, session1.status)
        assertEquals(37.77492, session1.latitude!!, 0.0001)

        // Idempotency: Triggering again while ACTIVE must return existing session without duplication
        val res2 = emergencyRepo.createEmergencySession(
            userId = "USER_1",
            triggerType = EmergencyTriggerType.HARDWARE_BLE_BUTTON,
            latitude = 37.77500,
            longitude = -122.41950,
            locationAccuracy = 5.0f
        )
        assertTrue(res2 is com.example.core.common.AppResult.Success)
        val session2 = (res2 as com.example.core.common.AppResult.Success).data
        assertEquals("Idempotent call should return the exact same session ID", session1.sessionId, session2.sessionId)

        // Resolve Emergency
        val resolveRes = emergencyRepo.updateSessionState(
            sessionId = session1.sessionId,
            newState = EmergencyState.RESOLVED,
            reason = "Audited user de-escalation"
        )
        assertTrue("Emergency state resolution must succeed", resolveRes is com.example.core.common.AppResult.Success)
        val resolved = (resolveRes as com.example.core.common.AppResult.Success).data
        assertEquals(EmergencyState.RESOLVED, resolved.status)
        assertNotNull("Resolved session must contain a timestamp", resolved.resolvedAt)
    }

    // TC-BLE-01 / TC-BLE-02: Hardware BLE Enrollment, Validation, and Heartbeat
    @Test
    fun testBleDeviceEnrollmentAndHeartbeat() = runBlocking {
        val device = GuardianDevice(
            deviceId = "dev-esp32-001",
            userId = "USER_1",
            name = "Tactical ESP32 Beacon",
            macAddress = "AA:BB:CC:DD:EE:FF",
            deviceType = DeviceType.ESP32_BLE_BEACON,
            enrollmentDate = System.currentTimeMillis(),
            isPaired = true,
            batteryLevel = 100
        )
        val enrollResult = deviceRepo.enrollDevice(device)
        assertTrue("Enrollment with valid MAC must succeed", enrollResult is com.example.core.common.AppResult.Success)

        // Heartbeat Continuity
        val now = System.currentTimeMillis()
        val heartbeatRes = deviceRepo.updateHeartbeat("dev-esp32-001", now, 88)
        assertTrue("Heartbeat update must succeed", heartbeatRes is com.example.core.common.AppResult.Success)

        val fetchedDevices = deviceRepo.getDevices("USER_1")
        assertEquals(1, fetchedDevices.size)
        assertEquals(88, fetchedDevices[0].batteryLevel)
    }

    // TC-SYNC-01: Offline Room Persistence and Queue Processing
    @Test
    fun testOfflineSyncQueueResilience() = runBlocking {
        // Enqueue session
        val session = emergencyRepo.createEmergencySession("USER_1", EmergencyTriggerType.MANUAL_SOS_BUTTON, 37.77, -122.41, 5f)
        val sessionId = (session as com.example.core.common.AppResult.Success).data.sessionId

        syncManager.enqueueForSync("emergency_session", sessionId)
        var pendingCount = 0
        val pendingSessions = database.emergencySessionDao().getPendingSyncSessions()
        assertTrue("Pending sync sessions should have at least 1 record", pendingSessions.isNotEmpty())

        val processedCount = syncManager.processPendingSync()
        assertTrue("Offline sync queue should process pending records", processedCount >= 1)

        val remainingPending = database.emergencySessionDao().getPendingSyncSessions()
        assertEquals("Queue should be fully synced", 0, remainingPending.size)
    }

    // TC-CONTACT-01: Contact Database Read/Write & Emergency Recipient Arming
    @Test
    fun testContactPersistenceAndToggling() = runBlocking {
        val contactDao = database.trustedContactDao()
        val entity = TrustedContactEntity(
            contactId = "ct-100",
            userId = "USER_1",
            name = "Agent Mulder",
            phoneNumber = "+12025550199",
            email = "fox@fbi.safety",
            relationship = ContactRelationship.SECURITY_TEAM.name,
            isEmergencyRecipient = true,
            priorityOrder = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        contactDao.insertContact(entity)

        val contacts = contactDao.getContactsForUser("USER_1")
        assertEquals(1, contacts.size)
        assertEquals("+12025550199", contacts[0].phoneNumber)
        assertTrue(contacts[0].isEmergencyRecipient)

        // Mute recipient
        contactDao.updateContact(contacts[0].copy(isEmergencyRecipient = false))
        val updatedContacts = contactDao.getContactsForUser("USER_1")
        assertEquals(false, updatedContacts[0].isEmergencyRecipient)
    }
}
