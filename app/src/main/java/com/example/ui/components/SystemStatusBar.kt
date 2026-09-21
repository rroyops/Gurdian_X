package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.NetworkStatus
import com.example.emergency.domain.EmergencySession
import com.example.emergency.domain.EmergencyState
import com.example.ui.theme.GuardianAlertDanger
import com.example.ui.theme.GuardianAmberWarning
import com.example.ui.theme.GuardianEmeraldSuccess

@Composable
fun SystemStatusBar(
    networkStatus: NetworkStatus,
    activeEmergency: EmergencySession?,
    modifier: Modifier = Modifier
) {
    val isEmergencyActive = activeEmergency != null && activeEmergency.status.isOngoing()
    val isOffline = networkStatus != NetworkStatus.AVAILABLE

    // Top banner indicator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_status_bar")
    ) {
        if (isEmergencyActive) {
            EmergencyActiveBanner(session = activeEmergency!!)
        } else {
            NetworkStatusIndicator(networkStatus = networkStatus)
        }
    }
}

@Composable
private fun EmergencyActiveBanner(session: EmergencySession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuardianAlertDanger)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("banner_emergency_active"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SOS ACTIVE [${session.status.name}] • SESSION #${session.sessionId.takeLast(8)}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun NetworkStatusIndicator(networkStatus: NetworkStatus) {
    val isOnline = networkStatus == NetworkStatus.AVAILABLE
    val isLosing = networkStatus == NetworkStatus.LOSING

    val (bg, textColor, icon, label) = when {
        isOnline -> Quad(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            GuardianEmeraldSuccess,
            Icons.Default.Wifi,
            "ONLINE • REAL-TIME DISPATCH"
        )
        isLosing -> Quad(
            GuardianAmberWarning.copy(alpha = 0.2f),
            GuardianAmberWarning,
            Icons.Default.CloudOff,
            "NETWORK UNSTABLE"
        )
        else -> Quad(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            GuardianAmberWarning,
            Icons.Default.CloudOff,
            "OFFLINE • LOCAL SAFETY ACTIVE"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("banner_network_status"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = textColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(
            text = "ENCRYPTION ARMED",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
