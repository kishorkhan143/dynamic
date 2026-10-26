package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScanState
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarning
import com.example.ui.theme.OrbitronFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenOverlay(
    scanState: ScanState,
    onTapScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()).uppercase()
    }

    val isSuccess = scanState == ScanState.SUCCESS
    val isScanning = scanState == ScanState.SCANNING

    val glowAlpha = remember { Animatable(0.2f) }
    LaunchedEffect(isScanning) {
        if (isScanning) {
            glowAlpha.animateTo(
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            glowAlpha.snapTo(0.2f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF030508),
                        Color(0xFF080D14),
                        Color(0xFF030508)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (scanState == ScanState.IDLE) {
                    onTapScreen()
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("lock_screen_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section below the Dynamic Island
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp), // Clearance for the top-center Dynamic Island
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Padlock Status Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSuccess) AuraSuccess.copy(alpha = 0.15f)
                            else AuraElectricAqua.copy(alpha = 0.1f)
                        )
                        .border(
                            1.dp,
                            if (isSuccess) AuraSuccess.copy(alpha = 0.5f)
                            else AuraElectricAqua.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Lock Status",
                        tint = if (isSuccess) AuraSuccess else AuraElectricAqua,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date
                Text(
                    text = currentDate,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    color = AuraElectricAqua,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Giant Clock
                Text(
                    text = currentTime,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Pill
                val statusText = when (scanState) {
                    ScanState.SCANNING -> "AUTHENTICATING FACE ID..."
                    ScanState.SUCCESS -> "FACE VERIFIED // UNLOCKED"
                    ScanState.RETRY -> "REALIGN FACE // RETRYING"
                    ScanState.ERROR -> "UNRECOGNIZED // TRY AGAIN"
                    ScanState.IDLE -> "TAP SCREEN TO SCAN FACE ID"
                }

                val statusColor = when (scanState) {
                    ScanState.SUCCESS -> AuraSuccess
                    ScanState.SCANNING -> AuraElectricAqua
                    ScanState.RETRY -> AuraWarning
                    ScanState.ERROR -> AuraError
                    ScanState.IDLE -> AuraTextSecondary
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F1520))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Bottom Shortcuts Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight shortcut
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x331E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Swipe up indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x80FFFFFF))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SWIPE UP TO OPEN",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 9.sp,
                            color = AuraTextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    // Camera shortcut
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x331E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
