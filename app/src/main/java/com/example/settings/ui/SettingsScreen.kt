package com.example.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.logging.AuditEvent
import com.example.ui.components.TacticalCard
import com.example.ui.theme.GuardianAmberWarning
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianEmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    auditCount: Int,
    pendingSyncCount: Int = 0,
    recentAuditEvents: List<AuditEvent> = emptyList(),
    onProcessSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var locationTrackingEnabled by remember { mutableStateOf(true) }
    var autoDispatchSmsEnabled by remember { mutableStateOf(true) }
    var tamperEvidenceEnabled by remember { mutableStateOf(true) }
    var showAuditViewer by remember { mutableStateOf(false) }

    if (showAuditViewer) {
        AuditTrailViewer(
            auditEvents = recentAuditEvents,
            onClose = { showAuditViewer = false }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Safety Hardware Switches
        TacticalCard(
            title = "Tactical & Emergency Triggers",
            icon = Icons.Default.Vibration,
            accentColor = GuardianCyanPrimary
        ) {
            SettingToggleRow(
                title = "Haptic Vibration on SOS",
                subtitle = "Vibrate device during countdown & trigger",
                checked = hapticFeedbackEnabled,
                onCheckedChange = { hapticFeedbackEnabled = it },
                testTag = "switch_haptic_sos"
            )
            SettingToggleRow(
                title = "Continuous GPS Coordinates",
                subtitle = "Attach latitude & longitude to emergency sessions",
                checked = locationTrackingEnabled,
                onCheckedChange = { locationTrackingEnabled = it },
                testTag = "switch_gps_tracking"
            )
            SettingToggleRow(
                title = "Discreet Emergency Dispatch",
                subtitle = "Notify trusted contacts silently without sirens",
                checked = autoDispatchSmsEnabled,
                onCheckedChange = { autoDispatchSmsEnabled = it },
                testTag = "switch_discreet_sms"
            )
        }

        // Real Emergency Alert & Gmail Engine Status
        TacticalCard(
            title = "Gmail / SMTP & Remote Alert Engine",
            icon = Icons.Default.Security,
            accentColor = GuardianCyanPrimary
        ) {
            val smtpConfig = remember { com.example.notifications.email.EmailConfigProvider.getSmtpConfig() }
            val hasGmailAuth = remember { com.example.notifications.email.EmailConfigProvider.isGmailConfigured() }
            val hasApiKey = remember { com.example.notifications.email.EmailConfigProvider.isApiConfigured() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gmail / SMTP Host:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${smtpConfig.host}:${smtpConfig.port}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gmail Account:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = smtpConfig.username.ifBlank { smtpConfig.senderEmail },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gmail App Password Auth:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasGmailAuth) "Armed (App Password Set)" else "Not Set in Secrets",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasGmailAuth) GuardianEmeraldSuccess else GuardianAmberWarning
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fallback REST Relay Key:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasApiKey) "Active" else "Unset",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasApiKey) GuardianEmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "When SOS triggers, GuardianX uses direct SMTP to dispatch emergency emails from your Gmail account via secure STARTTLS/SSL, detailing your coordinates, user identity, and live Google Maps hyperlinks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Offline Resilience & Synchronization Queue
        TacticalCard(
            title = "Offline Safety & Cloud Sync Engine",
            icon = Icons.Default.Sync,
            accentColor = GuardianEmeraldSuccess
        ) {
            Text(
                text = "GuardianX operates 100% offline. Emergency sessions and forensic evidence queue in Room database and synchronize once a network connection is established.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending Upload Queue:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$pendingSyncCount Record(s)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (pendingSyncCount > 0) GuardianCyanPrimary else GuardianEmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onProcessSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_process_sync"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardianCyanPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process Offline Sync Queue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Integrity & Cryptography
        TacticalCard(
            title = "Cryptographic Storage & Vault",
            icon = Icons.Default.Lock,
            accentColor = GuardianCyanPrimary
        ) {
            SettingToggleRow(
                title = "SHA-256 Tamper Evidence",
                subtitle = "Cryptographically chain all audit and session events",
                checked = tamperEvidenceEnabled,
                onCheckedChange = { tamperEvidenceEnabled = it },
                testTag = "switch_tamper_evidence"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stored Tamper-Evident Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$auditCount Event(s)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showAuditViewer = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_view_audit_trail"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GuardianCyanPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Cryptographic Audit Trail", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // About & Version Card
        TacticalCard(
            title = "Platform Identity",
            icon = Icons.Default.Info,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = "GuardianX Client v1.1.0 (Production Hardened)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Autonomous personal defense and cyber threat inspection system running Jetpack Compose and local SQLite Room persistence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GuardianCyanPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun AuditTrailViewer(
    auditEvents: List<AuditEvent>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    var selectedEvent by remember { mutableStateOf<AuditEvent?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("audit_trail_viewer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("button_back_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Audit Log Vault",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "${auditEvents.size} Records",
                style = MaterialTheme.typography.labelMedium,
                color = GuardianCyanPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (auditEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audit events recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(auditEvents) { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_event_${event.eventId}")
                            .clickable { selectedEvent = event },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.action.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GuardianCyanPrimary
                                )
                                Text(
                                    text = dateFormat.format(Date(event.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (event.details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Hash",
                                    tint = GuardianEmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SHA-256: ${event.integrityHash.take(16)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog displaying full cryptographic verification certificate for selected audit event
    val currentSelected = selectedEvent
    if (currentSelected != null) {
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GuardianEmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cryptographic Certificate",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("audit_certificate_dialog")
                ) {
                    Text(
                        text = "ACTION: ${currentSelected.action.name}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = GuardianCyanPrimary
                    )
                    Text(
                        text = "EVENT ID: ${currentSelected.eventId}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ACTOR: ${currentSelected.actorId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "TIMESTAMP: ${dateFormat.format(Date(currentSelected.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!currentSelected.resourceId.isNullOrEmpty()) {
                        Text(
                            text = "RESOURCE ID: ${currentSelected.resourceId}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (currentSelected.details.isNotEmpty()) {
                        Text(
                            text = "DETAILS: ${currentSelected.details}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FULL INTEGRITY SHA-256 HASH:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = currentSelected.integrityHash,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = GuardianEmeraldSuccess
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedEvent = null },
                    modifier = Modifier.testTag("button_dismiss_certificate")
                ) {
                    Text("Close", color = GuardianCyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
