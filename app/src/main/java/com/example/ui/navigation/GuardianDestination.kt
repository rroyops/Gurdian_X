package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

enum class GuardianDestination(
    val route: String,
    val titleResId: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
) {
    EMERGENCY(
        route = "emergency",
        titleResId = com.example.R.string.nav_emergency,
        activeIcon = Icons.Filled.Shield,
        inactiveIcon = Icons.Outlined.Shield,
        testTag = "nav_tab_emergency"
    ),
    CONTACTS(
        route = "contacts",
        titleResId = com.example.R.string.nav_contacts,
        activeIcon = Icons.Filled.Group,
        inactiveIcon = Icons.Outlined.Group,
        testTag = "nav_tab_contacts"
    ),
    DEVICES(
        route = "devices",
        titleResId = com.example.R.string.nav_devices,
        activeIcon = Icons.Filled.Bluetooth,
        inactiveIcon = Icons.Outlined.Bluetooth,
        testTag = "nav_tab_devices"
    ),
    PHISHING(
        route = "phishing",
        titleResId = com.example.R.string.nav_phishing,
        activeIcon = Icons.Filled.Security,
        inactiveIcon = Icons.Outlined.Security,
        testTag = "nav_tab_phishing"
    ),
    EVIDENCE(
        route = "evidence",
        titleResId = com.example.R.string.nav_evidence,
        activeIcon = Icons.Filled.FolderSpecial,
        inactiveIcon = Icons.Outlined.FolderSpecial,
        testTag = "nav_tab_evidence"
    ),
    SETTINGS(
        route = "settings",
        titleResId = com.example.R.string.nav_settings,
        activeIcon = Icons.Filled.Settings,
        inactiveIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}
