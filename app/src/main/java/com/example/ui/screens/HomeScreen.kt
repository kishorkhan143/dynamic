package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.model.BackgroundMode
import com.example.model.PresetCatalog
import com.example.model.ScanPreset
import com.example.model.ScanState
import com.example.ui.components.FaceScanRenderer
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraBorderBright
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraNeonViolet
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun HomeScreen(
    viewModel: AuraUnlockViewModel,
    onNavigateToAnimations: () -> Unit,
    onNavigateToStudio: () -> Unit
) {
    val studioConfig by viewModel.studioConfig.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val realtimeFaceData by viewModel.realtimeFaceData.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) {
            viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header & Futuristic Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AURA UNLOCK",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "BIOMETRIC HUD // SIMULATOR",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = AuraElectricAqua,
                    letterSpacing = 1.sp
                )
            }

            // Status chip
            Surface(
                color = studioConfig.accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, studioConfig.accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (scanState == ScanState.SCANNING) AuraElectricAqua else AuraSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (scanState == ScanState.SCANNING) "SCANNING" else "ONLINE",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = AuraTextPrimary,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Face Scan Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
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
                isPlaying = studioConfig.isPlaying,
                scanState = scanState,
                scanProgress = scanProgress,
                showTelemetryOverlay = true,
                realtimeFaceData = realtimeFaceData,
                onRealtimeFaceUpdate = { viewModel.updateRealtimeFaceData(it) }
            )

            // Live Camera / Face Recognition mode toggle chip on top-left of hero card
            val isCameraActive = studioConfig.backgroundMode == BackgroundMode.CAMERA && hasCameraPermission
            Surface(
                color = if (isCameraActive) Color(0xD9051515) else Color(0xD90E131A),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCameraActive) AuraElectricAqua.copy(alpha = 0.6f) else AuraBorder
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clickable {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            if (studioConfig.backgroundMode == BackgroundMode.CAMERA) {
                                viewModel.updateBackgroundMode(BackgroundMode.SILHOUETTE)
                            } else {
                                viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
                            }
                        }
                    }
                    .testTag("toggle_camera_realtime_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCameraActive) Icons.Default.CameraAlt else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = if (isCameraActive) AuraElectricAqua else AuraTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isCameraActive) "AI REALTIME CAM" else "ENABLE REALTIME CAM",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 9.sp,
                        color = if (isCameraActive) AuraElectricAqua else AuraTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Favorite Button on top right of hero card
            val isFav = favorites.contains(studioConfig.preset.id)
            IconButton(
                onClick = { viewModel.toggleFavorite(studioConfig.preset.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .testTag("favorite_button")
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFav) AuraElectricAqua else AuraTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Details Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = studioConfig.preset.name,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 16.sp,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = studioConfig.preset.category,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = AuraTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.triggerRetrySimulation() },
                    modifier = Modifier.testTag("retry_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Simulate Scan Retry",
                        tint = AuraTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNavigateToStudio,
                    modifier = Modifier.testTag("tune_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Open Studio",
                        tint = AuraElectricAqua
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Island Sensor Indicator Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C111A))
                .border(1.dp, AuraBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (scanState == ScanState.SUCCESS) AuraSuccess.copy(alpha = 0.2f)
                            else if (scanState == ScanState.SCANNING) AuraElectricAqua.copy(alpha = 0.2f)
                            else Color(0xFF161E2C)
                        )
                        .border(
                            1.dp,
                            if (scanState == ScanState.SUCCESS) AuraSuccess
                            else if (scanState == ScanState.SCANNING) AuraElectricAqua
                            else AuraBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (scanState == ScanState.SUCCESS) Icons.Default.CheckCircle
                        else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (scanState == ScanState.SUCCESS) AuraSuccess
                        else if (scanState == ScanState.SCANNING) AuraElectricAqua
                        else AuraTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FIXED TOP SENSOR // DYNAMIC ISLAND",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (scanState == ScanState.SCANNING) "SCANNING ACTIVE AT TOP NOTCH..."
                        else if (scanState == ScanState.SUCCESS) "AUTHENTICATION VERIFIED ✓"
                        else "Tap the top camera island or buttons below to trigger Face ID",
                        fontSize = 11.sp,
                        color = if (scanState == ScanState.SCANNING) AuraElectricAqua
                        else if (scanState == ScanState.SUCCESS) AuraSuccess
                        else AuraTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startScanSimulation() },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("preview_animation_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = studioConfig.accentColor,
                    contentColor = Color(0xFF003733)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RUN SCAN DEMO",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            Button(
                onClick = { viewModel.lockAndAuthenticate() },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("lock_and_unlock_flow_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF141A26),
                    contentColor = AuraTextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorderBright)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AuraElectricAqua
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LOCK & AUTH FLOW",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Preset Selector Row
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEATURED PRESETS",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    color = AuraTextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "VIEW ALL",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = AuraElectricAqua,
                    modifier = Modifier.clickable { onNavigateToAnimations() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PresetCatalog.ALL_PRESETS) { preset ->
                    val isSelected = preset.id == studioConfig.preset.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AuraSurfaceCard else Color(0xFF0F141C))
                            .border(
                                1.dp,
                                if (isSelected) preset.accentColor else AuraBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.selectPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(preset.accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preset.name,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                color = if (isSelected) AuraTextPrimary else AuraTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simulation & Privacy Notice Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0E131A))
                .border(1.dp, AuraBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Note",
                    tint = AuraElectricAqua,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SAFE SIMULATION PROTOCOL",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All scanning effects are procedural visual animations. Aura Unlock does not store biometric data or alter your device's lock-screen system authentication.",
                        fontSize = 12.sp,
                        color = AuraTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
