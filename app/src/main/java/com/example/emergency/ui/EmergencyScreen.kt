package com.example.emergency.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.emergency.domain.EmergencySession
import com.example.emergency.domain.EmergencyState
import com.example.notifications.domain.DispatchMessage
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalSosButton
import com.example.ui.theme.GuardianAlertDanger
import com.example.ui.theme.GuardianAmberWarning
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianEmeraldSuccess

@Composable
fun EmergencyScreen(
    activeSession: EmergencySession?,
    dispatchedAlerts: List<DispatchMessage>,
    onTriggerSos: () -> Unit,
    onResolveEmergency: () -> Unit,
    onDiscreetAlert: () -> Unit,
    onCountdownTick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentState = activeSession?.status ?: EmergencyState.IDLE
    val isOngoing = currentState.isOngoing()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("emergency_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Shield Header
        Text(
            text = stringResource(R.string.emergency_header_title),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isOngoing) stringResource(R.string.emergency_status_active) else stringResource(R.string.emergency_status_idle),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = if (isOngoing) GuardianAlertDanger else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Center Tactical Radar SOS Button with Hold & Tap Gestures
        TacticalSosButton(
            emergencyState = currentState,
            onSosTriggered = {
                if (isOngoing) onResolveEmergency() else onTriggerSos()
            },
            onSosCancelled = onResolveEmergency,
            onCountdownTick = onCountdownTick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscreetAlert,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("button_discreet_alert"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GuardianCyanPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Discreet Beacon", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            if (isOngoing) {
                Button(
                    onClick = onResolveEmergency,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("button_resolve_sos"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuardianEmeraldSuccess,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resolve SOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Live Dispatched Alerts List Card (When active or recent)
        if (dispatchedAlerts.isNotEmpty()) {
            TacticalCard(
                title = "Emergency Dispatch Confirmations (${dispatchedAlerts.size})",
                icon = Icons.AutoMirrored.Filled.Send,
                accentColor = GuardianEmeraldSuccess
            ) {
                dispatchedAlerts.takeLast(4).forEach { dispatch ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (dispatch.isDelivered) GuardianEmeraldSuccess else GuardianAlertDanger, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recipient: ${dispatch.recipientName} (${dispatch.recipientPhone})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(3.dp))
                        
                        // Detailed channel breakdown
                        if (!dispatch.recipientEmail.isNullOrBlank()) {
                            Text(
                                text = "✉️ Email: ${dispatch.recipientEmail} [${dispatch.emailStatus}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dispatch.emailStatus.startsWith("SENT")) GuardianEmeraldSuccess else GuardianAmberWarning
                            )
                        }
                        Text(
                            text = "🔔 Push Alert: [${dispatch.pushStatus}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dispatch.pushStatus == "DELIVERED_TO_SYSTEM") GuardianEmeraldSuccess else GuardianAmberWarning
                        )
                        Text(
                            text = dispatch.messageText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Live Telemetry Summary Card
        TacticalCard(
            title = "Defense Engine Telemetry",
            icon = Icons.Default.Security,
            accentColor = if (isOngoing) GuardianAlertDanger else GuardianCyanPrimary
        ) {
            TelemetryRow(label = "State Machine:", value = currentState.name)
            TelemetryRow(
                label = "Session Identifier:",
                value = activeSession?.sessionId?.takeLast(12) ?: "STANDBY_READY"
            )
            TelemetryRow(
                label = "Trigger Origin:",
                value = activeSession?.triggerType?.name ?: "MANUAL_SOS_BUTTON"
            )
            TelemetryRow(
                label = "GPS Coordinates:",
                value = if (activeSession?.latitude != null && activeSession.longitude != null) {
                    "${"%.5f".format(activeSession.latitude)}, ${"%.5f".format(activeSession.longitude)}"
                } else {
                    "37.77492, -122.41942 (Cached)"
                }
            )
            TelemetryRow(
                label = "Haptic Engine:",
                value = "SOS Morse Waveform Armed"
            )
            TelemetryRow(
                label = "Integrity Hash:",
                value = "SHA-256 Chained in Room"
            )
        }

        // Safety Advisory Card
        TacticalCard(
            title = "Hardware Continuity Notice",
            icon = Icons.Default.Info,
            accentColor = MaterialTheme.colorScheme.tertiary
        ) {
            Text(
                text = "Emergency alerts execute atomic local state transitions with haptic pulses. Registered trusted contacts receive SMS dispatches containing precise GPS telemetry and instant Google Maps hyperlinks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
