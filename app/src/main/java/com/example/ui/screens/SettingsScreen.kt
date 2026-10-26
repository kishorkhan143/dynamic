package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.BackgroundMode
import com.example.service.AppOpenAccessibilityService
import com.example.service.DynamicIslandOverlayService
import com.example.service.OverlayTrigger
import com.example.service.getIslandBackgroundColor
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
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: AuraUnlockViewModel
) {
    val studioConfig by viewModel.studioConfig.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val islandDarkness by viewModel.islandDarkness.collectAsState()
    val animationVelocity by viewModel.animationVelocity.collectAsState()
    val islandOffsetY by viewModel.islandOffsetY.collectAsState()
    val islandOffsetX by viewModel.islandOffsetX.collectAsState()
    val islandScale by viewModel.islandScale.collectAsState()
    val greenLightEnabled by viewModel.greenLightEnabled.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) {
            viewModel.updateBackgroundMode(BackgroundMode.CAMERA)
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isOverlayRunning by remember { mutableStateOf(DynamicIslandOverlayService.isRunning) }
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(AppOpenAccessibilityService.isEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
                isAccessibilityEnabled = AppOpenAccessibilityService.isEnabled(context)
                isOverlayRunning = DynamicIslandOverlayService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                    desc = "Required to render the Dynamic Island biometric pill at the top camera notch across all applications.",
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

                // 2. App-Open Detection (Accessibility Service)
                PermissionStatusItem(
                    icon = Icons.Default.TouchApp,
                    title = "App-Open Detection (Accessibility)",
                    desc = "Detects when any third-party app is launched to automatically trigger the Face ID animation from the top notch island, then return into the island.",
                    isGranted = isAccessibilityEnabled,
                    actionLabel = if (isAccessibilityEnabled) "ACTIVE" else "ENABLE IN SETTINGS",
                    onAction = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                // 3. Notification Permission (Background Service)
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

                // 4. Camera Sensor Permission
                PermissionStatusItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Front Optical Camera",
                    desc = "Powers in-app live camera preview and facial landmark detection. Never used in background overlays.",
                    isGranted = hasCameraPermission,
                    actionLabel = if (hasCameraPermission) "GRANTED" else "ALLOW CAMERA",
                    onAction = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                // 5. Biometric Hardware Authentication Trigger
                PermissionStatusItem(
                    icon = Icons.Default.Security,
                    title = "In-App Biometric Verification",
                    desc = "Test the biometric simulation and trigger the Dynamic Island expand animation.",
                    isGranted = true,
                    actionLabel = "TEST SCAN",
                    onAction = {
                        viewModel.startScanSimulation()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Island & Face ID Customization Card
        // Dynamic Island & Face ID Customization Card
        SettingGroupCard(title = "DYNAMIC ISLAND & FACE ID CONTROLS") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Always-On Top Center Camera Dynamic Island Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Always-On Camera Cutout Island",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (canDrawOverlays) {
                                if (isOverlayRunning) "Active — Always visible at top camera; stays alive on app close or recent task swipe" else "Enable to keep Dynamic Island at top center camera 24/7"
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

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 2. Dynamic Island Position Adjustment (Vertical Y & Horizontal X)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AuraElectricAqua,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "POSITION ADJUSTMENT (TOP & CENTER)",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTextPrimary
                            )
                        }
                        Text(
                            text = "Y: ${islandOffsetY}dp • X: ${islandOffsetX}dp",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraElectricAqua
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vertical Distance from Top Screen Edge (Y): ${islandOffsetY} dp",
                        fontSize = 11.sp,
                        color = AuraTextSecondary
                    )
                    Slider(
                        value = islandOffsetY.toFloat(),
                        onValueChange = { viewModel.updateIslandOffsetY(it.toInt()) },
                        valueRange = 0f..60f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("island_position_y_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AuraElectricAqua,
                            activeTrackColor = AuraElectricAqua,
                            inactiveTrackColor = Color(0xFF222B3A)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonePresetChip(
                            label = "0dp Top",
                            isSelected = islandOffsetY == 0,
                            onClick = { viewModel.updateIslandOffsetY(0) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "12dp Default",
                            isSelected = islandOffsetY == 12,
                            onClick = { viewModel.updateIslandOffsetY(12) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "24dp Mid",
                            isSelected = islandOffsetY == 24,
                            onClick = { viewModel.updateIslandOffsetY(24) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "40dp Low",
                            isSelected = islandOffsetY == 40,
                            onClick = { viewModel.updateIslandOffsetY(40) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Horizontal Offset (X): ${if (islandOffsetX > 0) "+$islandOffsetX" else "$islandOffsetX"} dp",
                        fontSize = 11.sp,
                        color = AuraTextSecondary
                    )
                    Slider(
                        value = islandOffsetX.toFloat(),
                        onValueChange = { viewModel.updateIslandOffsetX(it.toInt()) },
                        valueRange = -50f..50f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("island_position_x_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AuraElectricAqua,
                            activeTrackColor = AuraElectricAqua,
                            inactiveTrackColor = Color(0xFF222B3A)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonePresetChip(
                            label = "-25dp Left",
                            isSelected = islandOffsetX == -25,
                            onClick = { viewModel.updateIslandOffsetX(-25) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "0dp Centered",
                            isSelected = islandOffsetX == 0,
                            onClick = { viewModel.updateIslandOffsetX(0) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "+25dp Right",
                            isSelected = islandOffsetX == 25,
                            onClick = { viewModel.updateIslandOffsetX(25) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 3. Dynamic Island Size Reduction & Scaling Control
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AuraElectricAqua,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ISLAND SIZE (REDUCE / EXPAND)",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTextPrimary
                            )
                        }

                        val sizePct = (islandScale * 100).toInt()
                        Text(
                            text = "$sizePct% • ${(160 * islandScale).toInt()}x${(36 * islandScale).toInt()}dp",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraElectricAqua
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Slide to shrink and reduce the capsule size to match small camera pinholes",
                        fontSize = 11.sp,
                        color = AuraTextSecondary
                    )

                    Slider(
                        value = islandScale,
                        onValueChange = { viewModel.updateIslandScale(it) },
                        valueRange = 0.50f..1.30f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("island_size_scale_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AuraElectricAqua,
                            activeTrackColor = AuraElectricAqua,
                            inactiveTrackColor = Color(0xFF222B3A)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonePresetChip(
                            label = "60% Mini",
                            isSelected = (islandScale - 0.60f) in -0.05f..0.05f,
                            onClick = { viewModel.updateIslandScale(0.60f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "80% Compact",
                            isSelected = (islandScale - 0.80f) in -0.05f..0.05f,
                            onClick = { viewModel.updateIslandScale(0.80f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "100% Normal",
                            isSelected = (islandScale - 1.0f) in -0.05f..0.05f,
                            onClick = { viewModel.updateIslandScale(1.0f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "120% Large",
                            isSelected = (islandScale - 1.20f) in -0.05f..0.05f,
                            onClick = { viewModel.updateIslandScale(1.20f) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 4. Island Tone: Darker or Lighter Control (with pure OLED pitch black)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = AuraElectricAqua,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ISLAND TONE (DARKER / LIGHTER)",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTextPrimary
                            )
                        }

                        val toneDescription = when {
                            islandDarkness >= 0.85f -> "Pitch Black OLED"
                            islandDarkness >= 0.55f -> "Deep Charcoal"
                            islandDarkness >= 0.25f -> "Frosted Slate"
                            else -> "Lighter Titanium"
                        }
                        Text(
                            text = "${(islandDarkness * 100).toInt()}% • $toneDescription",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraElectricAqua
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Interactive Mini Preview Capsule
                    val previewBg = getIslandBackgroundColor(islandDarkness)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF080C12))
                            .border(1.dp, Color(0xFF161F2E), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewWidth = (120 * islandScale).coerceIn(60f, 160f).dp
                        val previewHeight = (28 * islandScale).coerceIn(16f, 38f).dp
                        Box(
                            modifier = Modifier
                                .width(previewWidth)
                                .height(previewHeight)
                                .clip(RoundedCornerShape(14.dp))
                                .background(previewBg)
                                .border(
                                    width = if (islandDarkness >= 0.70f) 0.8.dp else 1.dp,
                                    color = if (islandDarkness >= 0.70f) Color(0xFF222936) else if (islandDarkness < 0.45f) Color(0x66FFFFFF) else Color(0x6635F5E5),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Face glyph preview
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    val col = if (islandDarkness < 0.45f) Color.White else Color(0xFFB4BFCE)
                                    val b = 3f
                                    drawLine(col, Offset(0f, 0f), Offset(b, 0f), 1.2f)
                                    drawLine(col, Offset(0f, 0f), Offset(0f, b), 1.2f)
                                    drawLine(col, Offset(size.width, 0f), Offset(size.width - b, 0f), 1.2f)
                                    drawLine(col, Offset(size.width, 0f), Offset(size.width, b), 1.2f)
                                    drawLine(col, Offset(0f, size.height), Offset(b, size.height), 1.2f)
                                    drawLine(col, Offset(0f, size.height), Offset(0f, size.height - b), 1.2f)
                                    drawLine(col, Offset(size.width, size.height), Offset(size.width - b, size.height), 1.2f)
                                    drawLine(col, Offset(size.width, size.height), Offset(size.width, size.height - b), 1.2f)
                                }

                                // Center micro accent: Clean stealth dark bar (no green in center)
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(2.dp)
                                        .clip(CircleShape)
                                        .background(if (islandDarkness >= 0.70f) Color(0xFF141923) else Color(0xFF222B3A))
                                )

                                // Right-hand side: Green color privacy dot + Camera optic lens
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (greenLightEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E676))
                                                .border(0.6.dp, Color(0xFF69F0AE), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(2.5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8FFF0))
                                            )
                                        }
                                    }

                                    // Camera lens optic - pure deep dark
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF080C13))
                                            .border(0.8.dp, if (islandDarkness < 0.45f) Color(0xFF334155) else Color(0xFF1A2230), CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = islandDarkness,
                        onValueChange = { viewModel.updateIslandDarkness(it) },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("island_darkness_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AuraElectricAqua,
                            activeTrackColor = AuraElectricAqua,
                            inactiveTrackColor = Color(0xFF222B3A)
                        )
                    )

                    // Quick Tone Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonePresetChip(
                            label = "Lighter (0%)",
                            isSelected = islandDarkness <= 0.05f,
                            onClick = { viewModel.updateIslandDarkness(0.0f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "Slate (35%)",
                            isSelected = islandDarkness in 0.25f..0.45f,
                            onClick = { viewModel.updateIslandDarkness(0.35f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "Charcoal (70%)",
                            isSelected = islandDarkness in 0.60f..0.80f,
                            onClick = { viewModel.updateIslandDarkness(0.70f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "Pitch Black (100%)",
                            isSelected = islandDarkness >= 0.95f,
                            onClick = { viewModel.updateIslandDarkness(1.0f) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 5. Green Color Light Indicator Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Green Privacy Indicator Light",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (greenLightEnabled) "ON — Green dot displays on right side of Dynamic Island" else "OFF — Zero green light on resting island (Clean Stealth Notch)",
                            fontSize = 11.sp,
                            color = if (greenLightEnabled) AuraSuccess else AuraTextSecondary
                        )
                    }
                    Switch(
                        checked = greenLightEnabled,
                        onCheckedChange = { viewModel.updateGreenLightEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF003733),
                            checkedTrackColor = AuraSuccess,
                            uncheckedThumbColor = AuraTextMuted,
                            uncheckedTrackColor = Color(0xFF1B2230)
                        )
                    )
                }

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 6. Face ID Animation Velocity Control
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AuraElectricAqua,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FACE ID ANIMATION VELOCITY",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTextPrimary
                            )
                        }

                        val speedDescription = when {
                            animationVelocity <= 0.6f -> "Cinematic Slow"
                            animationVelocity <= 1.1f -> "Standard"
                            animationVelocity <= 1.6f -> "Snappy Fast"
                            else -> "Ultra-Rapid"
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.1f", animationVelocity)}x • $speedDescription",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraElectricAqua
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = animationVelocity,
                        onValueChange = { viewModel.updateAnimationVelocity(it) },
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("animation_velocity_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AuraElectricAqua,
                            activeTrackColor = AuraElectricAqua,
                            inactiveTrackColor = Color(0xFF222B3A)
                        )
                    )

                    // Quick Velocity Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonePresetChip(
                            label = "0.5x Slow",
                            isSelected = (animationVelocity - 0.5f) in -0.05f..0.05f,
                            onClick = { viewModel.updateAnimationVelocity(0.5f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "1.0x Normal",
                            isSelected = (animationVelocity - 1.0f) in -0.05f..0.05f,
                            onClick = { viewModel.updateAnimationVelocity(1.0f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "1.5x Fast",
                            isSelected = (animationVelocity - 1.5f) in -0.05f..0.05f,
                            onClick = { viewModel.updateAnimationVelocity(1.5f) },
                            modifier = Modifier.weight(1f)
                        )
                        TonePresetChip(
                            label = "2.0x Rapid",
                            isSelected = (animationVelocity - 2.0f) in -0.05f..0.05f,
                            onClick = { viewModel.updateAnimationVelocity(2.0f) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraBorder)
                )

                // 7. Preview System Overlay Button (Tests currently customized velocity, tone, position & size)
                Button(
                    onClick = {
                        if (Settings.canDrawOverlays(context)) {
                            if (!isOverlayRunning) {
                                DynamicIslandOverlayService.startService(context)
                                isOverlayRunning = true
                            }
                            OverlayTrigger.fire(OverlayTrigger.Source.MANUAL)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("preview_system_overlay_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF13222E),
                        contentColor = AuraElectricAqua
                    ),
                    border = BorderStroke(1.dp, AuraElectricAqua.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AuraElectricAqua,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PREVIEW FACE ID ANIMATION",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Test App-Open Trigger Button
                OutlinedButton(
                    onClick = {
                        if (Settings.canDrawOverlays(context)) {
                            if (!isOverlayRunning) {
                                DynamicIslandOverlayService.startService(context)
                                isOverlayRunning = true
                            }
                            OverlayTrigger.fire(OverlayTrigger.Source.APP_OPENED)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("test_app_open_trigger_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AuraTextSecondary
                    ),
                    border = BorderStroke(1.dp, AuraBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = AuraElectricAqua,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIMULATE APP-OPEN LAUNCH TRIGGER",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
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
                        text = "Live Front Camera Feed (In-App)",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        color = AuraTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hasCameraPermission) "Access granted // Optional in-app feed" else "Tap switch to request camera access",
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
                    desc = "All animations render locally via Jetpack Compose and GPU shaders on your device. Zero frames or telemetry are cached or transmitted."
                )
                PrivacyRow(
                    icon = Icons.Default.Security,
                    title = "Visual Animation Only",
                    desc = "Aura Unlock is a visual animation aesthetic suite. Android broadcasts a generic 'device unlocked' signal and does not reveal whether unlock occurred via Face, Fingerprint, or PIN. The animation does not verify or guarantee biometric identity."
                )
                PrivacyRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Zero Background Camera Usage",
                    desc = "Aura Unlock NEVER accesses or turns on camera hardware in the background or during system overlays. Camera sensors are only used when actively viewing the in-app camera mode."
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
        SettingGroupCard(title = "ANDROID BIOMETRIC & OVERLAY LIMITS") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Technical & Operating System Boundaries:",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
                Text(
                    text = "1. Security Isolation: Android's secure keyguard prevents third-party overlays on the locked screen. The Face ID unlock animation displays immediately upon device unlock.",
                    fontSize = 11.sp,
                    color = AuraTextSecondary,
                    lineHeight = 15.sp
                )
                Text(
                    text = "2. System Face Authentication: Android OS biometrics are managed entirely by system services. Third-party apps cannot override the stock system authentication dialogs.",
                    fontSize = 11.sp,
                    color = AuraTextSecondary,
                    lineHeight = 15.sp
                )
                Text(
                    text = "3. Touch Passthrough: The persistent top-center Dynamic Island overlay uses system touch passthrough flags so it never blocks camera interactions, status bar icons, or underlying apps.",
                    fontSize = 11.sp,
                    color = AuraTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Application Diagnostics & Reset
        SettingGroupCard(title = "DIAGNOSTICS & DEFAULTS") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reset All Customizations",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                        Text(
                            text = "Restores factory presets, island tone, and velocity",
                            fontSize = 11.sp,
                            color = AuraTextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraError),
                        border = BorderStroke(1.dp, AuraError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reset_all_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = AuraError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RESET",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "RESET ALL SETTINGS?",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will reset all animation parameters, island tone (to pitch black OLED), velocity (to 1.0x), and preset selections back to factory defaults.",
                    fontSize = 13.sp,
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraError,
                        contentColor = Color.White
                    )
                ) {
                    Text("RESET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", color = AuraTextMuted)
                }
            },
            containerColor = AuraSurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TonePresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF142E3B) else Color(0xFF141A25))
            .border(
                1.dp,
                if (isSelected) AuraElectricAqua else Color(0xFF222B3B),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = OrbitronFontFamily,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AuraElectricAqua else AuraTextSecondary,
            maxLines = 1
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
            .clip(RoundedCornerShape(16.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, AuraBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AuraElectricAqua,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
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
            border = BorderStroke(
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
