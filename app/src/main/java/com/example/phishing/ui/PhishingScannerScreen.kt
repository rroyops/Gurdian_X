package com.example.phishing.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.core.common.AppResult
import com.example.phishing.domain.PhishingScanResult
import com.example.phishing.domain.PhishingScanner
import com.example.phishing.domain.RiskLevel
import com.example.ui.components.TacticalCard
import com.example.ui.theme.GuardianAlertDanger
import com.example.ui.theme.GuardianAmberWarning
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianEmeraldSuccess
import kotlinx.coroutines.launch

@Composable
fun PhishingScannerScreen(
    scanner: PhishingScanner,
    onSaveThreatToVault: ((PhishingScanResult) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputContent by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<PhishingScanResult?>(null) }
    var vaultSaved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("phishing_scanner_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.phishing_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.phishing_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Input Box
        OutlinedTextField(
            value = inputContent,
            onValueChange = { 
                inputContent = it 
                vaultSaved = false
            },
            label = { Text("URL or Suspicious SMS Content") },
            placeholder = { Text(stringResource(R.string.phishing_input_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("input_phishing_content"),
            maxLines = 4
        )

        Button(
            onClick = {
                if (inputContent.isNotBlank()) {
                    vaultSaved = false
                    scope.launch {
                        val result = if (inputContent.startsWith("http://") || inputContent.startsWith("https://")) {
                            scanner.analyzeUrl(inputContent)
                        } else {
                            scanner.analyzeText(inputContent)
                        }
                        if (result is AppResult.Success) {
                            scanResult = result.data
                        }
                    }
                }
            },
            enabled = inputContent.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("button_execute_scan"),
            colors = ButtonDefaults.buttonColors(
                containerColor = GuardianCyanPrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.phishing_analyze_button), fontWeight = FontWeight.Bold)
        }

        // Quick Preset Test Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    inputContent = "http://secure-paypal-login.com.free-auth.ru/verify"
                    vaultSaved = false
                    scope.launch {
                        val result = scanner.analyzeUrl(inputContent)
                        if (result is AppResult.Success) {
                            scanResult = result.data
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).testTag("button_phish_preset_malicious")
            ) {
                Text("Test Threat", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    inputContent = "https://github.com/security/overview"
                    vaultSaved = false
                    scope.launch {
                        val result = scanner.analyzeUrl(inputContent)
                        if (result is AppResult.Success) {
                            scanResult = result.data
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).testTag("button_phish_preset_safe")
            ) {
                Text("Test Safe URL", fontSize = 11.sp)
            }
        }

        // Result Card
        val currentResult = scanResult
        if (currentResult != null) {
            ScanResultDisplayCard(
                result = currentResult,
                vaultSaved = vaultSaved,
                onSaveToVault = onSaveThreatToVault?.let { callback ->
                    {
                        callback(currentResult)
                        vaultSaved = true
                    }
                }
            )
        } else {
            TacticalCard(
                title = "Heuristic Protection Active",
                icon = Icons.Default.Security,
                accentColor = GuardianCyanPrimary
            ) {
                Text(
                    text = "The GuardianX heuristic engine checks for suspicious top-level domains, lookalike domains (typosquatting), urgent social engineering keywords, and invalid SSL flags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScanResultDisplayCard(
    result: PhishingScanResult,
    vaultSaved: Boolean = false,
    onSaveToVault: (() -> Unit)? = null
) {
    val (color, icon) = when (result.riskLevel) {
        RiskLevel.MINIMAL_OBSERVED_RISK -> GuardianEmeraldSuccess to Icons.Default.CheckCircle
        RiskLevel.SUSPICIOUS, RiskLevel.INCONCLUSIVE -> GuardianAmberWarning to Icons.Default.Warning
        RiskLevel.HIGH_RISK, RiskLevel.CRITICAL_THREAT -> GuardianAlertDanger to Icons.Default.Dangerous
    }

    TacticalCard(
        title = "Inspection Verdict",
        icon = icon,
        accentColor = color
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${result.riskLevel.name} VERDICT",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Analysis: ${result.explanation}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Recommended: ${result.recommendedAction}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (result.indicators.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Detected Indicators: ${result.indicators.joinToString { it.title }}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = GuardianAlertDanger
            )
        }

        // Vault Preservation Button for suspicious or threatening findings
        if (onSaveToVault != null && (result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.CRITICAL_THREAT || result.riskLevel == RiskLevel.SUSPICIOUS)) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSaveToVault,
                enabled = !vaultSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_save_threat_vault"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (vaultSaved) GuardianEmeraldSuccess else GuardianCyanPrimary
                )
            ) {
                Icon(
                    imageVector = if (vaultSaved) Icons.Default.Check else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (vaultSaved) "Committed to Evidence Vault" else "Commit Forensic Report to Vault",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
