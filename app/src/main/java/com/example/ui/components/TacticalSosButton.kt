package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.domain.EmergencyState
import com.example.ui.theme.GuardianAlertDanger
import com.example.ui.theme.GuardianCyanPrimary
import kotlinx.coroutines.launch

@Composable
fun TacticalSosButton(
    emergencyState: EmergencyState,
    onSosTriggered: () -> Unit,
    onSosCancelled: () -> Unit = {},
    onCountdownTick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActive = emergencyState.isOngoing()
    val scope = rememberCoroutineScope()

    var isPressing by remember { mutableStateOf(false) }
    val holdProgress = remember { Animatable(0f) }

    // Pulsing radar animation for active emergency state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 800 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val outerRingColor = if (isActive) GuardianAlertDanger else GuardianCyanPrimary
    val innerBgColors = if (isActive) {
        listOf(GuardianAlertDanger, Color(0xFFB71C1C))
    } else {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    }

    Box(
        modifier = modifier
            .size(250.dp)
            .scale(pulseScale)
            .testTag("tactical_sos_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring background
        Box(
            modifier = Modifier
                .size(244.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = outerRingColor.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Hold-to-trigger countdown indicator progress ring
        if (isPressing && !isActive) {
            CircularProgressIndicator(
                progress = { holdProgress.value },
                modifier = Modifier
                    .size(236.dp)
                    .testTag("sos_hold_progress_indicator"),
                color = GuardianAlertDanger,
                strokeWidth = 6.dp,
                trackColor = Color.Transparent
            )
        }

        // Middle indicator ring
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        listOf(outerRingColor, outerRingColor.copy(alpha = 0.2f), outerRingColor)
                    ),
                    shape = CircleShape
                )
        )

        // Core Action Button with tap / hold gesture detection
        Box(
            modifier = Modifier
                .size(180.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = outerRingColor)
                .clip(CircleShape)
                .background(Brush.radialGradient(innerBgColors))
                .border(
                    width = 2.dp,
                    color = if (isActive) Color.White.copy(alpha = 0.8f) else outerRingColor,
                    shape = CircleShape
                )
                .pointerInput(isActive) {
                    detectTapGestures(
                        onTap = {
                            if (isActive) {
                                onSosCancelled()
                            } else {
                                onSosTriggered()
                            }
                        },
                        onPress = {
                            if (!isActive) {
                                isPressing = true
                                onCountdownTick()
                                val job = scope.launch {
                                    holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
                                    )
                                    if (holdProgress.value >= 0.99f) {
                                        onSosTriggered()
                                    }
                                }
                                tryAwaitRelease()
                                isPressing = false
                                job.cancel()
                                holdProgress.snapTo(0f)
                            }
                        }
                    )
                }
                .testTag("sos_trigger_touch_target"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS Icon",
                    tint = if (isActive) Color.White else outerRingColor,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isActive) "ACTIVE" else if (isPressing) "ARMING..." else "SOS",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isActive) "TAP TO RESOLVE" else if (isPressing) "HOLD TO FIRE" else "TAP / HOLD 1.5S",
                    color = if (isActive) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
