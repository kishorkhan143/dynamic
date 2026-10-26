package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.ParticleDensity
import com.example.model.PresetCatalog
import com.example.model.ScanPreset
import com.example.ui.components.FaceScanRenderer
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraBorderBright
import com.example.ui.theme.AuraCoolWhite
import com.example.ui.theme.AuraCrimsonCore
import com.example.ui.theme.AuraCyberCyan
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraMatrixAmber
import com.example.ui.theme.AuraNeonViolet
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun StudioScreen(
    viewModel: AuraUnlockViewModel
) {
    val studioConfig by viewModel.studioConfig.collectAsState()
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

    val cyberColors = listOf(
        AuraElectricAqua to "Aqua",
        AuraNeonViolet to "Violet",
        AuraCyberCyan to "Cyan",
        AuraMatrixAmber to "Amber",
        AuraCrimsonCore to "Crimson",
        AuraCoolWhite to "Ice"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
    ) {
        // Studio Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ANIMATION STUDIO",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "REAL-TIME HUD CALIBRATION LAB",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = AuraElectricAqua,
                    letterSpacing = 0.8.sp
                )
            }

            IconButton(
                onClick = { viewModel.resetStudioDefaults() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141922))
                    .border(1.dp, AuraBorder, RoundedCornerShape(12.dp))
                    .testTag("reset_studio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Defaults",
                    tint = AuraTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Face Scan Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transport Controls (Play / Pause / Replay)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.triggerRetrySimulation() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121720))
                    .border(1.dp, AuraBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Simulate Scan Retry",
                    tint = AuraTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { viewModel.togglePlayPause() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = studioConfig.accentColor,
                    contentColor = Color(0xFF003733)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = if (studioConfig.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (studioConfig.isPlaying) "PAUSE ENGINE" else "RESUME ENGINE",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { viewModel.startScanSimulation() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121720))
                    .border(1.dp, AuraBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Simulate Scan Success",
                    tint = AuraElectricAqua
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preset Switcher Chips
        Text(
            text = "SELECT PRESET",
            fontFamily = OrbitronFontFamily,
            fontSize = 11.sp,
            color = AuraTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PresetCatalog.ALL_PRESETS) { preset ->
                val isSelected = preset.id == studioConfig.preset.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) studioConfig.accentColor.copy(alpha = 0.2f) else Color(0xFF10151E))
                        .border(
                            1.dp,
                            if (isSelected) studioConfig.accentColor else AuraBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        // Background Mode Selector (Silhouette vs Camera)
        Text(
            text = "PREVIEW BACKGROUND FEED",
            fontFamily = OrbitronFontFamily,
            fontSize = 11.sp,
            color = AuraTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isSilhouette = studioConfig.backgroundMode == BackgroundMode.SILHOUETTE
            val isCamera = studioConfig.backgroundMode == BackgroundMode.CAMERA

            // Silhouette Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSilhouette) AuraSurfaceCard else Color(0xFF0F141C))
                    .border(1.dp, if (isSilhouette) studioConfig.accentColor else AuraBorder, RoundedCornerShape(14.dp))
                    .clickable { viewModel.updateBackgroundMode(BackgroundMode.SILHOUETTE) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = if (isSilhouette) studioConfig.accentColor else AuraTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SILHOUETTE",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = if (isSilhouette) AuraTextPrimary else AuraTextSecondary,
                        fontWeight = if (isSilhouette) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Camera Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCamera) AuraSurfaceCard else Color(0xFF0F141C))
                    .border(1.dp, if (isCamera) studioConfig.accentColor else AuraBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        if (hasCameraPermission) {
                            viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (isCamera) studioConfig.accentColor else AuraTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasCameraPermission) "FRONT CAMERA" else "ENABLE CAMERA",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = if (isCamera) AuraTextPrimary else AuraTextSecondary,
                        fontWeight = if (isCamera) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Accent Color Palette Selector
        Text(
            text = "ACCENT NEON COLOR",
            fontFamily = OrbitronFontFamily,
            fontSize = 11.sp,
            color = AuraTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            cyberColors.forEach { (color, name) ->
                val isSelected = studioConfig.accentColor == color
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.updateAccentColor(color) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 9.sp,
                        color = if (isSelected) AuraTextPrimary else AuraTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Parameter Sliders Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AuraSurfaceCard)
                .border(1.dp, AuraBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                // Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SCAN VELOCITY",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = AuraTextPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", studioConfig.speed)}x",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = studioConfig.accentColor
                    )
                }
                Slider(
                    value = studioConfig.speed,
                    onValueChange = { viewModel.updateSpeed(it) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = studioConfig.accentColor,
                        activeTrackColor = studioConfig.accentColor,
                        inactiveTrackColor = Color(0xFF222B3A)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Glow Intensity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GLOW INTENSITY",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = AuraTextPrimary
                    )
                    Text(
                        text = "${(studioConfig.glowIntensity * 100).toInt()}%",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = studioConfig.accentColor
                    )
                }
                Slider(
                    value = studioConfig.glowIntensity,
                    onValueChange = { viewModel.updateGlow(it) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = studioConfig.accentColor,
                        activeTrackColor = studioConfig.accentColor,
                        inactiveTrackColor = Color(0xFF222B3A)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scan Line Width Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LINE THICKNESS",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = AuraTextPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", studioConfig.scanLineWidth)} dp",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = studioConfig.accentColor
                    )
                }
                Slider(
                    value = studioConfig.scanLineWidth,
                    onValueChange = { viewModel.updateLineWidth(it) },
                    valueRange = 1.0f..6.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = studioConfig.accentColor,
                        activeTrackColor = studioConfig.accentColor,
                        inactiveTrackColor = Color(0xFF222B3A)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Particle Density Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CYBER PARTICLES",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        color = AuraTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ParticleDensity.values().forEach { density ->
                            val isSel = studioConfig.particleDensity == density
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) studioConfig.accentColor else Color(0xFF141A24))
                                    .clickable { viewModel.updateParticleDensity(density) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = density.label,
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color(0xFF003733) else AuraTextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
