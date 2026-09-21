package com.example.evidence.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.evidence.domain.EvidenceRecord
import com.example.evidence.domain.EvidenceType
import com.example.ui.components.TacticalEmptyState
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianEmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EvidenceVaultScreen(
    evidenceList: List<EvidenceRecord>,
    onAddIncidentNote: (notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedRecordForDetail by remember { mutableStateOf<EvidenceRecord?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<EvidenceType?>(null) }

    val filteredList = remember(evidenceList, selectedTypeFilter) {
        if (selectedTypeFilter == null) evidenceList else evidenceList.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        modifier = modifier.testTag("evidence_vault_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNoteDialog = true },
                containerColor = GuardianCyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_evidence_note")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Incident Note")
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
                text = stringResource(R.string.evidence_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.evidence_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Evidence Type Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${evidenceList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GuardianCyanPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                items(EvidenceType.values()) { type ->
                    val count = evidenceList.count { it.type == type }
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                        label = { Text("${type.name.replace("_", " ")} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GuardianCyanPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                TacticalEmptyState(
                    icon = Icons.Default.FolderSpecial,
                    title = if (selectedTypeFilter != null) "No Records For Category" else "Forensic Vault Empty",
                    description = stringResource(R.string.evidence_empty_notice),
                    actionLabel = "Record Incident Note",
                    onActionClick = { showNoteDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.evidenceId }) { record ->
                        EvidenceRecordCard(
                            record = record,
                            onClick = { selectedRecordForDetail = record }
                        )
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        RecordNoteDialog(
            onDismiss = { showNoteDialog = false },
            onConfirm = { note ->
                onAddIncidentNote(note)
                showNoteDialog = false
            }
        )
    }

    selectedRecordForDetail?.let { record ->
        EvidenceDetailDialog(
            record = record,
            onDismiss = { selectedRecordForDetail = null }
        )
    }
}

@Composable
fun EvidenceRecordCard(
    record: EvidenceRecord,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("evidence_record_${record.evidenceId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.type) {
                        EvidenceType.AUDIO_RECORDING -> Icons.Default.Mic
                        EvidenceType.PHOTO_CAPTURE -> Icons.Default.PhotoCamera
                        EvidenceType.LOCATION_TRAIL, EvidenceType.SENSOR_TELEMETRY -> Icons.Default.GpsFixed
                        EvidenceType.INCIDENT_NOTES -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = GuardianCyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.type.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified SHA-256",
                        tint = GuardianEmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Path: ${record.localFilePath.takeLast(28)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "SHA-256: ${record.sha256Checksum.take(16)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = GuardianCyanPrimary
                )
            }
        }
    }
}

@Composable
fun EvidenceDetailDialog(
    record: EvidenceRecord,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("dialog_evidence_detail"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Forensic Record Certificate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailItem(label = "Record ID", value = record.evidenceId)
                DetailItem(label = "Associated Session", value = record.sessionId)
                DetailItem(label = "Evidence Type", value = record.type.name)
                DetailItem(label = "Local Storage Path", value = record.localFilePath)
                DetailItem(label = "File Size", value = "${record.fileSizeBytes} bytes")
                DetailItem(label = "Captured Timestamp", value = dateFormat.format(Date(record.capturedAt)))

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Cryptographic Digest (SHA-256):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = record.sha256Checksum,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = GuardianCyanPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GuardianCyanPrimary, contentColor = Color.Black)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RecordNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Secure Incident Note", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Notes are stored with an immutable SHA-256 checksum in local encrypted Room storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Incident Observations / Suspect Details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("input_evidence_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (noteText.isNotBlank()) {
                        onConfirm(noteText)
                    }
                },
                enabled = noteText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GuardianCyanPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_confirm_save_evidence")
            ) {
                Text("Lock & Hash Note", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
