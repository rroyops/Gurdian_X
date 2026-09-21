package com.example.devices.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.devices.domain.DeviceType
import com.example.devices.domain.GuardianDevice
import com.example.ui.components.TacticalEmptyState
import com.example.ui.theme.GuardianAlertDanger
import com.example.ui.theme.GuardianAmberWarning
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianEmeraldSuccess

@Composable
fun DevicesScreen(
    devices: List<GuardianDevice>,
    onPairDevice: (name: String, mac: String, type: DeviceType) -> Unit,
    onUnpairDevice: (String) -> Unit,
    onSendHeartbeat: (deviceId: String) -> Unit = {},
    onDiscreetTrigger: (deviceId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPairDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("devices_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPairDialog = true },
                containerColor = GuardianCyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_pair_device")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Pair Device")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.devices_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.devices_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (devices.isEmpty()) {
                TacticalEmptyState(
                    icon = Icons.Default.BluetoothSearching,
                    title = "No Hardware Enrolled",
                    description = stringResource(R.string.devices_empty_notice),
                    actionLabel = "Pair ESP32 Beacon",
                    onActionClick = { showPairDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices, key = { it.deviceId }) { device ->
                        DeviceCardItem(
                            device = device,
                            onUnpair = { onUnpairDevice(device.deviceId) },
                            onSendHeartbeat = { onSendHeartbeat(device.deviceId) },
                            onDiscreetTrigger = { onDiscreetTrigger(device.deviceId) }
                        )
                    }
                }
            }
        }
    }

    if (showPairDialog) {
        PairDeviceDialog(
            onDismiss = { showPairDialog = false },
            onConfirm = { name, mac, type ->
                onPairDevice(name, mac, type)
                showPairDialog = false
            }
        )
    }
}

@Composable
fun DeviceCardItem(
    device: GuardianDevice,
    onUnpair: () -> Unit,
    onSendHeartbeat: () -> Unit = {},
    onDiscreetTrigger: () -> Unit = {}
) {
    val isConnected = device.isPaired

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.deviceId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isConnected) GuardianEmeraldSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (device.deviceType) {
                            DeviceType.SMARTWATCH -> Icons.Default.Watch
                            DeviceType.ESP32_BLE_BEACON -> Icons.Default.Bluetooth
                            else -> Icons.Default.DevicesOther
                        },
                        contentDescription = null,
                        tint = if (isConnected) GuardianEmeraldSuccess else GuardianAmberWarning,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "MAC: ${device.macAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isConnected) "STATUS: PAIRED & ACTIVE" else "STATUS: ENROLLED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isConnected) GuardianEmeraldSuccess else GuardianAmberWarning
                        )
                        device.batteryLevel?.let { battery ->
                            Text(
                                text = "• $battery% BATTERY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (battery > 20) GuardianCyanPrimary else GuardianAlertDanger
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onUnpair,
                    modifier = Modifier.testTag("unpair_device_${device.deviceId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Unpair",
                        tint = GuardianAlertDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Affordances: Continuity Ping & Discreet SOS Simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSendHeartbeat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_ping_heartbeat_${device.deviceId}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GuardianCyanPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Continuity Ping", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onDiscreetTrigger,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_discreet_sos_${device.deviceId}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GuardianAlertDanger,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger SOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PairDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mac: String, type: DeviceType) -> Unit
) {
    var name by remember { mutableStateOf("ESP32-SOS-BEACON") }
    var mac by remember { mutableStateOf("AA:BB:CC:11:22:33") }
    val selectedType by remember { mutableStateOf(DeviceType.ESP32_BLE_BEACON) }

    val isMacValid = remember(mac) {
        mac.isBlank() || com.example.core.security.SecuritySanitizer.isValidMacAddress(mac)
    }
    val canSubmit = name.isNotBlank() && mac.isNotBlank() && isMacValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Enroll Guardian Device", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_device_name")
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address (e.g. AA:BB:CC:11:22:33)") },
                    isError = !isMacValid && mac.isNotBlank(),
                    supportingText = if (!isMacValid && mac.isNotBlank()) {
                        { Text("Format: XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_device_mac")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(name, mac, selectedType)
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = GuardianCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_confirm_pair_device")
            ) {
                Text("Enroll Device", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
