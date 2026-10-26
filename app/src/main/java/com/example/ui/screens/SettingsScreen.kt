package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.ui.platform.LocalContext
import com.example.service.DynamicIslandOverlayService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarning
import com.example.ui.theme.OrbitronFontFamily
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun SettingsScreen(
    viewModel: AuraUnlockViewModel
) {
    val studioConfig by viewModel.studioConfig.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) {
            viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
        }
    }

    val isDynamicIslandEnabled by viewModel.isDynamicIslandEnabled.collectAsState()
    val context = LocalContext.current
    var isOverlayRunning by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val canDrawOverlays = Settings.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
    ) {
        // Settings Header
        Text(
            text = "SYSTEM SETTINGS",
            fontFamily = OrbitronFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AuraTextPrimary,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "CONFIG // PRIVACY // OS PERMISSIONS",
            fontFamily = OrbitronFontFamily,
            fontSize = 11.sp,
            color = AuraElectricAqua,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Android Permission Hub Card
        SettingGroupCard(title = "ANDROID PERMISSIONS DASHBOARD") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. Overlay Permission (Display over other apps)
                PermissionStatusItem(
                    icon = Icons.Default.Visibility,
                    title = "Display Over Other Apps (Overlay)",
                    desc = "Required to float the Dynamic Island camera notch pill across all apps.",
                    isGranted = canDrawOverlays,
                    actionLabel = if (canDrawOverlays) "GRANTED" else "GRANT IN SETTINGS",
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                // 2. Notification Permission (Background Service)
                PermissionStatusItem(
                    icon = Icons.Default.Notifications,
                    title = "Background Notification Service",
                    desc = "Keeps the Dynamic Island notch service persistent on Android 13+.",
                    isGranted = hasNotificationPermission,
                    actionLabel = if (hasNotificationPermission) "ACTIVE" else "ENABLE",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                // 3. Camera Sensor Permission
                PermissionStatusItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Front Optical Camera",
                    desc = "Powers live camera view and real-time facial feature landmark detection.",
                    isGranted = hasCameraPermission,
                    actionLabel = if (hasCameraPermission) "GRANTED" else "ALLOW CAMERA",
                    onAction = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                // 4. Biometric Hardware Authentication Trigger
                PermissionStatusItem(
                    icon = Icons.Default.Security,
                    title = "Biometric Hardware Verification",
                    desc = "Test hardware biometric authentication and trigger the Dynamic Island animation.",
                    isGranted = true,
                    actionLabel = "TEST FACE ID",
                    onAction = {
                        viewModel.startScanSimulation()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Island Setting Card
        SettingGroupCard(title = "CAMERA CUTOUT DISPLAY") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // In-App Permanent Dynamic Island
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Always Present In-App",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Permanent top-center black capsule; expands to Face ID square on scan",
                            fontSize = 11.sp,
                            color = AuraTextSecondary
                        )
                    }
                    Switch(
                        checked = isDynamicIslandEnabled,
                        onCheckedChange = { viewModel.setDynamicIslandEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003733),
                            checkedTrackColor = AuraElectricAqua,
                            uncheckedThumbColor = AuraTextMuted,
                            uncheckedTrackColor = Color(0xFF1B2230)
                        )
                    )
                }

                // System-Wide Floating Island (When App Is Closed / Outside App)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Float On Screen When App Closed",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (Settings.canDrawOverlays(context)) {
                                if (isOverlayRunning) "Overlay active at top of screen" else "Enable to float pill over other apps"
                            } else {
                                "Requires Android 'Display over other apps' permission"
                            },
                            fontSize = 11.sp,
                            color = if (isOverlayRunning) AuraSuccess else AuraTextSecondary
                        )
                    }
                    Switch(
                        checked = isOverlayRunning,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Settings.canDrawOverlays(context)) {
                                    DynamicIslandOverlayService.startService(context)
                                    isOverlayRunning = true
                                } else {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            } else {
                                DynamicIslandOverlayService.stopService(context)
                                isOverlayRunning = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003733),
                            checkedTrackColor = AuraElectricAqua,
                            uncheckedThumbColor = AuraTextMuted,
                            uncheckedTrackColor = Color(0xFF1B2230)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preference Card: Camera Access
        SettingGroupCard(title = "OPTICAL HARDWARE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Front Camera Feed",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        color = AuraTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasCameraPermission) "Access granted // Optional overlay" else "Tap switch to request camera access",
                        fontSize = 11.sp,
                        color = if (hasCameraPermission) AuraSuccess else AuraTextSecondary
                    )
                }
                Switch(
                    checked = hasCameraPermission && studioConfig.backgroundMode == BackgroundMode.CAMERA,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (hasCameraPermission) {
                                viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            viewModel.updateBackgroundMode(BackgroundMode.SILHOUETTE)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF003733),
                        checkedTrackColor = AuraElectricAqua,
                        uncheckedThumbColor = AuraTextMuted,
                        uncheckedTrackColor = Color(0xFF1B2230)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Default Animation Preset
        SettingGroupCard(title = "DEFAULT STARTUP PRESET") {
            Column {
                Text(
                    text = "Currently Active: ${studioConfig.preset.name}",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    color = studioConfig.accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Switch presets dynamically from the Gallery or Studio anytime.",
                    fontSize = 11.sp,
                    color = AuraTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Manifesto
        SettingGroupCard(title = "PRIVACY & SECURITY PROTOCOL") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrivacyRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Strictly On-Device Processing",
                    desc = "All animations run purely via GPU shaders on your device. Zero camera frames are saved, cached, or transmitted."
                )
                PrivacyRow(
                    icon = Icons.Default.Security,
                    title = "Visual Simulation Only",
                    desc = "Aura Unlock is a futuristic visual animation studio. It does not perform actual facial biometric identification."
                )
                PrivacyRow(
                    icon = Icons.Default.Lock,
                    title = "No Cloud / No Account",
                    desc = "Your preferences and favorite animations are stored securely in local device storage."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OS Limitations & Feasibility Disclosure
        SettingGroupCard(title = "ANDROID BIOMETRIC LIMITATIONS") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Why Aura Unlock operates in-app:",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
                Text(
                    text = "• Native Lock Screen Isolation: Android OS executes face-unlock authentication inside an isolated Trusted Execution Environment (TEE) hardware enclave. Standard third-party apps cannot hook into or replace native system lock-screen animations.\n\n• Device Security Protection: Aura Unlock strictly complies with Android security rules and never attempts to bypass lock-screens or tamper with system biometric prompts.\n\n• Floating Overlay Feasibility: While system-alert overlays can be drawn over normal apps with user permission, Android security deliberately blocks overlays from rendering over the secure lock screen.",
                    fontSize = 12.sp,
                    color = AuraTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reset Settings Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_all_settings_button"),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraError.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = AuraError,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RESET ALL PREFERENCES",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = AuraError,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Version
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AURA UNLOCK // v1.0.0",
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                color = AuraTextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "COMPOSE CANVAS ENGINE // HARDWARE ACCELERATED",
                fontFamily = OrbitronFontFamily,
                fontSize = 9.sp,
                color = AuraTextMuted
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "RESET PREFERENCES?",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 14.sp,
                    color = AuraTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will restore default animation speeds, colors, and clear favorites.",
                    fontSize = 12.sp,
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                    }
                ) {
                    Text("RESET", fontFamily = OrbitronFontFamily, color = AuraError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", fontFamily = OrbitronFontFamily, color = AuraTextSecondary)
                }
            },
            containerColor = Color(0xFF141924)
        )
    }
}

@Composable
private fun SettingGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, AuraBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                color = AuraElectricAqua,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PrivacyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AuraElectricAqua,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = AuraTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PermissionStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) AuraSuccess else AuraElectricAqua,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = AuraTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF0E2218) else Color(0xFF141F2E),
                contentColor = if (isGranted) AuraSuccess else AuraElectricAqua
            ),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isGranted) AuraSuccess.copy(alpha = 0.4f) else AuraElectricAqua.copy(alpha = 0.4f)
            ),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = actionLabel,
                fontFamily = OrbitronFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
