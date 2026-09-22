package com.example.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contacts.domain.ContactRelationship
import com.example.contacts.domain.ContactRepository
import com.example.contacts.domain.TrustedContact
import com.example.core.common.AppResult
import com.example.core.di.ServiceLocator
import com.example.core.hardware.HapticFeedbackController
import com.example.core.logging.AuditAction
import com.example.core.logging.AuditEvent
import com.example.core.logging.AuditSeverity
import com.example.core.network.NetworkStatus
import com.example.devices.domain.DeviceRepository
import com.example.devices.domain.DeviceType
import com.example.devices.domain.GuardianDevice
import com.example.emergency.domain.EmergencyRepository
import com.example.emergency.domain.EmergencySession
import com.example.emergency.domain.EmergencyState
import com.example.emergency.domain.EmergencyTriggerType
import com.example.evidence.data.EvidenceRepositoryImpl
import com.example.evidence.domain.EvidenceChecksumCalculator
import com.example.evidence.domain.EvidenceRecord
import com.example.evidence.domain.EvidenceType
import com.example.notifications.domain.DispatchMessage
import com.example.notifications.domain.EmergencyNotificationDispatcher
import com.example.offline.domain.OfflineSyncManager
import com.example.phishing.domain.EnhancedPhishingScanner
import com.example.phishing.domain.HeuristicPhishingScanner
import com.example.phishing.domain.PhishingScanResult
import com.example.phishing.domain.PhishingScanner
import com.example.ui.navigation.GuardianDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val emergencyRepository: EmergencyRepository = ServiceLocator.emergencyRepository,
    private val contactRepository: ContactRepository = ServiceLocator.contactRepository,
    private val deviceRepository: DeviceRepository = ServiceLocator.deviceRepository,
    private val evidenceRepository: EvidenceRepositoryImpl = ServiceLocator.evidenceRepository,
    private val offlineSyncManager: OfflineSyncManager = ServiceLocator.offlineSyncManager,
    private val hapticController: HapticFeedbackController = ServiceLocator.hapticController,
    private val notificationDispatcher: EmergencyNotificationDispatcher = ServiceLocator.notificationDispatcher,
    private val locationProvider: com.example.location.domain.LocationProvider = ServiceLocator.locationProvider
) : ViewModel() {

    val phishingScanner: PhishingScanner = EnhancedPhishingScanner()
    private val currentUserId = "LOCAL_USER"

    private val _currentDestination = MutableStateFlow(GuardianDestination.EMERGENCY)
    val currentDestination: StateFlow<GuardianDestination> = _currentDestination.asStateFlow()

    val networkStatus: StateFlow<NetworkStatus> = ServiceLocator.networkStatusObserver
        .observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceLocator.networkStatusObserver.currentStatus)

    val contacts: StateFlow<List<TrustedContact>> = contactRepository.observeContacts(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val devices: StateFlow<List<GuardianDevice>> = deviceRepository.observeDevices(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEmergency: StateFlow<EmergencySession?> = emergencyRepository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val evidenceList: StateFlow<List<EvidenceRecord>> = evidenceRepository.observeAllEvidence()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = offlineSyncManager.observePendingSyncCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _dispatchedAlerts = MutableStateFlow<List<DispatchMessage>>(emptyList())
    val dispatchedAlerts: StateFlow<List<DispatchMessage>> = _dispatchedAlerts.asStateFlow()

    private val _auditEventsCount = MutableStateFlow(0)
    val auditEventsCount: StateFlow<Int> = _auditEventsCount.asStateFlow()

    private val _recentAuditEvents = MutableStateFlow<List<AuditEvent>>(emptyList())
    val recentAuditEvents: StateFlow<List<AuditEvent>> = _recentAuditEvents.asStateFlow()

    init {
        refreshAuditCount()
        viewModelScope.launch {
            val existing = contactRepository.getContacts(currentUserId)
            if (existing.isEmpty()) {
                contactRepository.addContact(
                    TrustedContact(
                        contactId = "primary_guardian_01",
                        userId = currentUserId,
                        name = "Primary Guardian Contact",
                        phoneNumber = "+1 (555) 911-0001",
                        email = "oisheebiswasarmy07@gmail.com",
                        relationship = ContactRelationship.FAMILY,
                        isEmergencyRecipient = true,
                        priorityOrder = 1
                    )
                )
            }
        }
    }

    fun navigateTo(destination: GuardianDestination) {
        _currentDestination.value = destination
    }

    fun onCountdownTick() {
        hapticController.vibrateCountdownTick()
    }

    fun triggerEmergency(triggerType: EmergencyTriggerType = EmergencyTriggerType.MANUAL_SOS_BUTTON) {
        viewModelScope.launch {
            hapticController.vibrateSosPulse()

            val locationResult = locationProvider.getLastKnownLocation()
            val (lat, lng, acc) = if (locationResult is AppResult.Success) {
                Triple(locationResult.data.latitude, locationResult.data.longitude, locationResult.data.accuracyMeters)
            } else {
                Triple(37.77492, -122.41942, 4.2f)
            }

            val result = emergencyRepository.createEmergencySession(
                userId = currentUserId,
                triggerType = triggerType,
                latitude = lat,
                longitude = lng,
                locationAccuracy = acc
            )

            if (result is AppResult.Success) {
                val session = result.data

                // Automatically capture GPS telemetry evidence record into vault
                recordEvidenceSnapshot(
                    sessionId = session.sessionId,
                    type = EvidenceType.LOCATION_TRAIL,
                    dataString = "LAT: $lat, LNG: $lng, ACCURACY: ${acc}m"
                )

                // Enqueue in offline sync queue
                offlineSyncManager.enqueueForSync("emergency_session", session.sessionId)

                // Dispatch notification to enrolled contacts who are active recipients
                val currentContacts = contacts.value.ifEmpty { contactRepository.getContacts(currentUserId) }
                val activeRecipients = currentContacts.filter { it.isEmergencyRecipient }.ifEmpty {
                    listOf(
                        TrustedContact(
                            contactId = "primary_guardian_01",
                            userId = currentUserId,
                            name = "Primary Guardian Contact",
                            phoneNumber = "+1 (555) 911-0001",
                            email = "oisheebiswasarmy07@gmail.com",
                            relationship = ContactRelationship.FAMILY,
                            isEmergencyRecipient = true,
                            priorityOrder = 1
                        )
                    )
                }
                val dispatchResult = notificationDispatcher.dispatchEmergencyAlert(session, activeRecipients)
                if (dispatchResult is AppResult.Success) {
                    _dispatchedAlerts.value = dispatchResult.data
                    dispatchResult.data.forEach { msg ->
                        recordAudit(
                            action = AuditAction.ALERT_DISPATCHED,
                            resourceId = session.sessionId,
                            details = "Alert -> ${msg.recipientName} (${msg.recipientPhone}). Email: ${msg.emailStatus}, Push: ${msg.pushStatus}"
                        )
                    }
                }

                recordAudit(
                    action = AuditAction.EMERGENCY_STARTED,
                    resourceId = session.sessionId,
                    details = "SOS triggered via ${triggerType.name} with ${activeRecipients.size} contacts notified (Lat: $lat, Lng: $lng)"
                )
            }
        }
    }

    fun resolveEmergency() {
        val current = activeEmergency.value ?: return
        viewModelScope.launch {
            hapticController.vibrateResolutionConfirmation()
            val result = emergencyRepository.updateSessionState(
                sessionId = current.sessionId,
                newState = EmergencyState.RESOLVED,
                reason = "Resolved by user via dashboard interface"
            )
            if (result is AppResult.Success) {
                recordAudit(
                    action = AuditAction.EMERGENCY_RESOLVED,
                    resourceId = current.sessionId,
                    details = "Emergency resolved and dispatches concluded"
                )
            }
        }
    }

    fun triggerDiscreetAlert() {
        triggerEmergency(EmergencyTriggerType.HARDWARE_BLE_BUTTON)
    }

    fun addContact(name: String, phone: String, email: String, relationship: ContactRelationship) {
        viewModelScope.launch {
            val contact = TrustedContact(
                contactId = ServiceLocator.idGenerator.newId(),
                userId = currentUserId,
                name = name,
                phoneNumber = phone,
                email = email,
                relationship = relationship,
                priorityOrder = (contacts.value.size + 1),
                createdAt = ServiceLocator.timeProvider.currentTimeMillis(),
                updatedAt = ServiceLocator.timeProvider.currentTimeMillis()
            )
            contactRepository.addContact(contact)
            recordAudit(AuditAction.CONTACT_ADDED, contact.contactId, "Added trusted contact: $name")
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(contactId)
            recordAudit(AuditAction.CONTACT_REMOVED, contactId, "Removed trusted contact")
        }
    }

    fun toggleContactEmergencyRecipient(contactId: String) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.contactId == contactId } ?: return@launch
            val updated = contact.copy(
                isEmergencyRecipient = !contact.isEmergencyRecipient,
                updatedAt = ServiceLocator.timeProvider.currentTimeMillis()
            )
            contactRepository.updateContact(updated)
            recordAudit(
                action = AuditAction.SECURITY_SETTING_CHANGED,
                resourceId = contactId,
                details = "Updated dispatch state: ${if (updated.isEmergencyRecipient) "ARMED" else "MUTED"}"
            )
        }
    }

    fun pairDevice(name: String, mac: String, type: DeviceType) {
        viewModelScope.launch {
            val device = GuardianDevice(
                deviceId = ServiceLocator.idGenerator.newId(),
                userId = currentUserId,
                name = name,
                macAddress = mac,
                deviceType = type,
                enrollmentDate = ServiceLocator.timeProvider.currentTimeMillis(),
                isPaired = true,
                batteryLevel = 100
            )
            deviceRepository.enrollDevice(device)
            recordAudit(AuditAction.DEVICE_ENROLLED, device.deviceId, "Enrolled hardware: $name ($mac)")
        }
    }

    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            deviceRepository.revokeDevice(deviceId)
            recordAudit(AuditAction.DEVICE_REVOKED, deviceId, "Revoked hardware device")
        }
    }

    fun sendDeviceHeartbeat(deviceId: String) {
        viewModelScope.launch {
            val now = ServiceLocator.timeProvider.currentTimeMillis()
            deviceRepository.updateHeartbeat(deviceId, now, 95)
            recordAudit(AuditAction.SECURITY_SETTING_CHANGED, deviceId, "Hardware continuity heartbeat received from beacon")
        }
    }

    fun addIncidentNote(note: String) {
        val currentSessionId = activeEmergency.value?.sessionId ?: "incident_${ServiceLocator.timeProvider.currentTimeMillis()}"
        recordEvidenceSnapshot(
            sessionId = currentSessionId,
            type = EvidenceType.INCIDENT_NOTES,
            dataString = note
        )
    }

    fun commitThreatToVault(result: PhishingScanResult) {
        val currentSessionId = activeEmergency.value?.sessionId ?: "threat_${ServiceLocator.timeProvider.currentTimeMillis()}"
        val threatReport = buildString {
            appendLine("SECURITY INSPECTION THREAT VERDICT")
            appendLine("Target: ${result.target}")
            appendLine("Risk Level: ${result.riskLevel.name}")
            appendLine("Explanation: ${result.explanation}")
            appendLine("Recommended Action: ${result.recommendedAction}")
            appendLine("Analyzed At: ${result.analyzedAt}")
            if (result.indicators.isNotEmpty()) {
                appendLine("Indicators: ${result.indicators.joinToString { "${it.code}: ${it.title}" }}")
            }
        }
        recordEvidenceSnapshot(
            sessionId = currentSessionId,
            type = EvidenceType.INCIDENT_NOTES,
            dataString = threatReport
        )
    }

    fun processSyncQueue() {
        viewModelScope.launch {
            val syncedCount = offlineSyncManager.processPendingSync()
            recordAudit(
                action = AuditAction.SECURITY_SETTING_CHANGED,
                resourceId = "SYNC_ENGINE",
                details = "Offline queue processed: $syncedCount record(s) synced"
            )
        }
    }

    private fun recordEvidenceSnapshot(sessionId: String, type: EvidenceType, dataString: String) {
        viewModelScope.launch {
            val evidenceId = ServiceLocator.idGenerator.newId()
            val checksum = EvidenceChecksumCalculator.calculateSha256ForText(dataString)
            val record = EvidenceRecord(
                evidenceId = evidenceId,
                sessionId = sessionId,
                type = type,
                localFilePath = "/vault/evidence_${evidenceId.take(8)}.dat",
                remoteStorageUri = null,
                sha256Checksum = checksum,
                fileSizeBytes = dataString.toByteArray(Charsets.UTF_8).size.toLong(),
                capturedAt = ServiceLocator.timeProvider.currentTimeMillis()
            )
            evidenceRepository.createEvidenceRecord(record)
            recordAudit(
                action = AuditAction.EVIDENCE_CAPTURED,
                resourceId = evidenceId,
                details = "Captured ${type.name} record with SHA-256 $checksum"
            )
        }
    }

    private fun recordAudit(action: AuditAction, resourceId: String, details: String) {
        viewModelScope.launch {
            ServiceLocator.auditLogger.recordEvent(
                actorId = currentUserId,
                action = action,
                severity = AuditSeverity.INFO,
                resourceId = resourceId,
                details = details
            )
            refreshAuditCount()
        }
    }

    private fun refreshAuditCount() {
        viewModelScope.launch {
            val events = ServiceLocator.auditLogger.getRecentAuditEvents(100)
            _auditEventsCount.value = events.size
            _recentAuditEvents.value = events
        }
    }
}
