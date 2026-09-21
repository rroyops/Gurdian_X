package com.example.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.contacts.ui.ContactsScreen
import com.example.devices.ui.DevicesScreen
import com.example.emergency.ui.EmergencyScreen
import com.example.evidence.ui.EvidenceVaultScreen
import com.example.phishing.ui.PhishingScannerScreen
import com.example.settings.ui.SettingsScreen
import com.example.ui.components.GuardianNavigationBar
import com.example.ui.components.SystemStatusBar
import com.example.ui.navigation.GuardianDestination

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val activeEmergency by viewModel.activeEmergency.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val evidenceList by viewModel.evidenceList.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val dispatchedAlerts by viewModel.dispatchedAlerts.collectAsState()
    val auditCount by viewModel.auditEventsCount.collectAsState()
    val recentAuditEvents by viewModel.recentAuditEvents.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("guardian_main_scaffold"),
        topBar = {
            SystemStatusBar(
                networkStatus = networkStatus,
                activeEmergency = activeEmergency
            )
        },
        bottomBar = {
            GuardianNavigationBar(
                currentDestination = currentDestination,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "navigation_content_animation"
            ) { destination ->
                when (destination) {
                    GuardianDestination.EMERGENCY -> EmergencyScreen(
                        activeSession = activeEmergency,
                        dispatchedAlerts = dispatchedAlerts,
                        onTriggerSos = { viewModel.triggerEmergency() },
                        onResolveEmergency = { viewModel.resolveEmergency() },
                        onDiscreetAlert = { viewModel.triggerDiscreetAlert() },
                        onCountdownTick = { viewModel.onCountdownTick() }
                    )
                    GuardianDestination.CONTACTS -> ContactsScreen(
                        contacts = contacts,
                        onAddContact = { name, phone, email, rel ->
                            viewModel.addContact(name, phone, email, rel)
                        },
                        onDeleteContact = { viewModel.deleteContact(it) },
                        onToggleEmergencyRecipient = { viewModel.toggleContactEmergencyRecipient(it) }
                    )
                    GuardianDestination.DEVICES -> DevicesScreen(
                        devices = devices,
                        onPairDevice = { name, mac, type ->
                            viewModel.pairDevice(name, mac, type)
                        },
                        onUnpairDevice = { viewModel.unpairDevice(it) },
                        onSendHeartbeat = { viewModel.sendDeviceHeartbeat(it) },
                        onDiscreetTrigger = { viewModel.triggerDiscreetAlert() }
                    )
                    GuardianDestination.PHISHING -> PhishingScannerScreen(
                        scanner = viewModel.phishingScanner,
                        onSaveThreatToVault = { viewModel.commitThreatToVault(it) }
                    )
                    GuardianDestination.EVIDENCE -> EvidenceVaultScreen(
                        evidenceList = evidenceList,
                        onAddIncidentNote = { viewModel.addIncidentNote(it) }
                    )
                    GuardianDestination.SETTINGS -> SettingsScreen(
                        auditCount = auditCount,
                        pendingSyncCount = pendingSyncCount,
                        recentAuditEvents = recentAuditEvents,
                        onProcessSync = { viewModel.processSyncQueue() }
                    )
                }
            }
        }
    }
}
