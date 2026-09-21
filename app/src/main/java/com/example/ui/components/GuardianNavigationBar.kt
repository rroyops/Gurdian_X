package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.GuardianDestination
import com.example.ui.theme.GuardianCyanContainer
import com.example.ui.theme.GuardianCyanPrimary
import com.example.ui.theme.GuardianDarkSurface
import com.example.ui.theme.GuardianTextMuted
import com.example.ui.theme.GuardianTextPrimary

@Composable
fun GuardianNavigationBar(
    currentDestination: GuardianDestination,
    onNavigate: (GuardianDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("guardian_bottom_navigation_bar"),
        windowInsets = WindowInsets.navigationBars,
        containerColor = GuardianDarkSurface,
        tonalElevation = 8.dp
    ) {
        GuardianDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.activeIcon else destination.inactiveIcon,
                        contentDescription = stringResource(destination.titleResId),
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.titleResId),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GuardianCyanPrimary,
                    selectedTextColor = GuardianCyanPrimary,
                    indicatorColor = GuardianCyanContainer,
                    unselectedIconColor = GuardianTextMuted,
                    unselectedTextColor = GuardianTextMuted
                ),
                modifier = Modifier.testTag(destination.testTag)
            )
        }
    }
}
