package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun FullScreenPreviewDialog(
    viewModel: AuraUnlockViewModel,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val studioConfig by viewModel.studioConfig.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val realtimeFaceData by viewModel.realtimeFaceData.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5050608))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Padlock Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSuccess = scanState == ScanState.SUCCESS
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Lock Status",
                        tint = if (isSuccess) AuraSuccess else studioConfig.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "UNLOCKED // DEMO" else "SCANNING FACE",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = if (isSuccess) AuraSuccess else AuraTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161B26))
                        .border(1.dp, AuraBorder, CircleShape)
                        .testTag("close_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AuraTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Scanning Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                FaceScanRenderer(
                    preset = studioConfig.preset,
                    modifier = Modifier.fillMaxSize(),
                    accentColor = studioConfig.accentColor,
                    speedMultiplier = studioConfig.speed,
                    glowIntensity = studioConfig.glowIntensity,
                    scanLineWidth = studioConfig.scanLineWidth,
                    particleDensity = studioConfig.particleDensity,
                    backgroundMode = studioConfig.backgroundMode,
                    hasCameraPermission = hasCameraPermission,
                    isPlaying = true,
                    scanState = scanState,
                    scanProgress = scanProgress,
                    showTelemetryOverlay = true,
                    realtimeFaceData = realtimeFaceData,
                    onRealtimeFaceUpdate = { viewModel.updateRealtimeFaceData(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Status & Control HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F141E))
                    .border(1.dp, AuraBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusText = when (scanState) {
                    ScanState.IDLE -> "READY TO SCAN"
                    ScanState.SCANNING -> "ACQUIRING BIOMETRICS... ${(scanProgress * 100).toInt()}%"
                    ScanState.SUCCESS -> "SIMULATION COMPLETE: VERIFIED"
                    ScanState.RETRY -> "SIMULATION: REALIGN FACE"
                    ScanState.ERROR -> "SIMULATION: NOT RECOGNIZED"
                }

                val statusColor = when (scanState) {
                    ScanState.SUCCESS -> AuraSuccess
                    ScanState.RETRY -> AuraWarning
                    ScanState.ERROR -> AuraError
                    ScanState.SCANNING -> studioConfig.accentColor
                    ScanState.IDLE -> AuraTextSecondary
                }

                Text(
                    text = statusText,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.startScanSimulation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("rescan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = studioConfig.accentColor,
                            contentColor = Color(0xFF003733)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRIGGER SCAN",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerRetrySimulation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B2332),
                            contentColor = AuraTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "TEST RETRY",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "NON-BIOMETRIC VISUAL DEMO // SECURE SANDBOX",
                fontFamily = OrbitronFontFamily,
                fontSize = 9.sp,
                color = AuraTextMuted,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
