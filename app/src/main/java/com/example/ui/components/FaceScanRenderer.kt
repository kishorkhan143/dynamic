package com.example.ui.components

import android.content.Context
import android.graphics.RectF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.example.detection.RealtimeFaceAnalyzer
import com.example.detection.RealtimeFaceData
import com.example.detection.RecognitionStatus
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.BackgroundMode
import com.example.model.ParticleDensity
import com.example.model.ScanPreset
import com.example.model.ScanState
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraCyberCyan
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraNeonViolet
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraWarning
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SimParticle(
    var xRatio: Float,
    var yRatio: Float,
    val speed: Float,
    val radius: Float,
    val alphaOffset: Float
)

@Composable
fun FaceScanRenderer(
    preset: ScanPreset,
    modifier: Modifier = Modifier,
    accentColor: Color = preset.accentColor,
    speedMultiplier: Float = 1.0f,
    glowIntensity: Float = 0.75f,
    scanLineWidth: Float = 2.5f,
    particleDensity: ParticleDensity = ParticleDensity.MEDIUM,
    backgroundMode: BackgroundMode = BackgroundMode.SILHOUETTE,
    hasCameraPermission: Boolean = false,
    isPlaying: Boolean = true,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: Float = 0f,
    showTelemetryOverlay: Boolean = true,
    realtimeFaceData: RealtimeFaceData = RealtimeFaceData(),
    onRealtimeFaceUpdate: (RealtimeFaceData) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Animated phases
    val primaryScanPhase = remember { Animatable(0f) }
    val secondaryRotation = remember { Animatable(0f) }
    val pulsePhase = remember { Animatable(0f) }

    // Particles buffer
    val particles = remember(particleDensity) {
        val rand = Random(42)
        List(particleDensity.count) {
            SimParticle(
                xRatio = 0.2f + rand.nextFloat() * 0.6f,
                yRatio = 0.15f + rand.nextFloat() * 0.7f,
                speed = 0.003f + rand.nextFloat() * 0.007f,
                radius = 1.2f + rand.nextFloat() * 2.8f,
                alphaOffset = rand.nextFloat()
            )
        }
    }

    // Animation loops driven by speedMultiplier
    val durationScan = (2400 / speedMultiplier.coerceIn(0.5f, 3.0f)).toInt()
    val durationRot = (6000 / speedMultiplier.coerceIn(0.5f, 3.0f)).toInt()
    val durationPulse = (1800 / speedMultiplier.coerceIn(0.5f, 3.0f)).toInt()

    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying) {
            primaryScanPhase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = durationScan, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying) {
            secondaryRotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = durationRot, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying) {
            pulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = durationPulse, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AuraBackground)
            .border(1.dp, AuraBorder, RoundedCornerShape(24.dp))
    ) {
        // Background layer: CameraX Preview or Silhouette
        if (backgroundMode == BackgroundMode.CAMERA && hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build().also { analyzer ->
                                    analyzer.setAnalyzer(
                                        ContextCompat.getMainExecutor(ctx),
                                        RealtimeFaceAnalyzer { data ->
                                            onRealtimeFaceUpdate(data)
                                        }
                                    )
                                }
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        } catch (_: Exception) {
                            // Fallback to default camera if front not available
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                    .build().also { analyzer ->
                                        analyzer.setAnalyzer(
                                            ContextCompat.getMainExecutor(ctx),
                                            RealtimeFaceAnalyzer { data ->
                                                onRealtimeFaceUpdate(data)
                                            }
                                        )
                                    }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                            } catch (_: Exception) {}
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            // Dark vignette overlay over camera feed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0xCC050608), Color(0xF2050608))
                        )
                    )
            )
        }

        // Hardware Canvas Layer for Face Mesh & HUD Animations
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate live center & face bounds if detected by real-time ML Kit
            val isLiveDetected = realtimeFaceData.isFaceDetected && realtimeFaceData.faceBounds != null
            val liveBounds = realtimeFaceData.faceBounds

            val cx = if (isLiveDetected && liveBounds != null) {
                ((liveBounds.left + liveBounds.right) / 2f) * w
            } else {
                w / 2f
            }

            val cy = if (isLiveDetected && liveBounds != null) {
                ((liveBounds.top + liveBounds.bottom) / 2f) * h
            } else {
                h / 2f
            }

            val liveFaceW = if (isLiveDetected && liveBounds != null) {
                ((liveBounds.right - liveBounds.left) * w).coerceIn(w * 0.28f, w * 0.85f)
            } else {
                w * 0.44f
            }

            val liveFaceH = if (isLiveDetected && liveBounds != null) {
                ((liveBounds.bottom - liveBounds.top) * h).coerceIn(h * 0.28f, h * 0.85f)
            } else {
                h * 0.48f
            }

            val scanY = if (isLiveDetected && liveBounds != null) {
                val top = liveBounds.top * h
                val span = (liveBounds.bottom - liveBounds.top) * h
                top + primaryScanPhase.value * span
            } else {
                h * (0.15f + primaryScanPhase.value * 0.7f)
            }

            val rotAngle = secondaryRotation.value
            val pulse = pulsePhase.value

            val statusColor = when (scanState) {
                ScanState.SUCCESS -> AuraSuccess
                ScanState.RETRY -> AuraWarning
                ScanState.ERROR -> AuraError
                else -> accentColor
            }

            // 1. Draw Biometric Face Silhouette & Mesh
            // When in SILHOUETTE mode or when a live face is tracked, render the biometric mesh
            if (backgroundMode == BackgroundMode.SILHOUETTE || !hasCameraPermission || isLiveDetected) {
                drawBiometricSilhouette(
                    cx = cx,
                    cy = cy,
                    w = w,
                    h = h,
                    accentColor = statusColor,
                    glowIntensity = glowIntensity,
                    scanLineWidth = scanLineWidth,
                    presetId = preset.id,
                    pulse = pulse,
                    scanY = scanY,
                    faceW = liveFaceW,
                    faceH = liveFaceH,
                    liveLeftEye = if (isLiveDetected) realtimeFaceData.leftEyePos?.let { Offset(it.x * w, it.y * h) } else null,
                    liveRightEye = if (isLiveDetected) realtimeFaceData.rightEyePos?.let { Offset(it.x * w, it.y * h) } else null,
                    liveNose = if (isLiveDetected) realtimeFaceData.nosePos?.let { Offset(it.x * w, it.y * h) } else null,
                    liveMouth = if (isLiveDetected) realtimeFaceData.mouthPos?.let { Offset(it.x * w, it.y * h) } else null
                )
            }

            // 2. Preset-Specific Animation Drawing
            when (preset.id) {
                "aqua_pulse" -> drawAquaPulseEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
                "neural_scan" -> drawNeuralScanEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
                "holographic_face" -> drawHolographicEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
                "cyber_grid" -> drawCyberGridEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, rotAngle
                )
                "minimal_orbit" -> drawMinimalOrbitEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, rotAngle
                )
                "neon_contour" -> drawNeonContourEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
                "digital_particles" -> drawDigitalParticlesEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, particles, pulse
                )
                "aurora_scan" -> drawAuroraScanEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
                else -> drawAquaPulseEffect(
                    cx, cy, w, h, scanY, statusColor, glowIntensity, scanLineWidth, pulse
                )
            }

            // 3. Draw Cyber Reticles & Corner Targeting Brackets
            drawCyberHUDFrame(
                w = w,
                h = h,
                accentColor = statusColor,
                lineWidth = scanLineWidth,
                glowIntensity = glowIntensity,
                scanState = scanState,
                scanProgress = scanProgress,
                faceBounds = if (isLiveDetected) liveBounds else null
            )
        }

        // 4. Telemetry Readout & Status Header
        if (showTelemetryOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Telemetry
                Text(
                    text = "AURA // ${preset.name.uppercase()}",
                    color = accentColor.copy(alpha = 0.9f),
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                val stateLabel = if (realtimeFaceData.isFaceDetected) {
                    when (realtimeFaceData.status) {
                        RecognitionStatus.VERIFIED -> "FACE VERIFIED // UNLOCKED"
                        RecognitionStatus.TRACKING -> "ANALYZING // ${(scanProgress * 100).toInt()}%"
                        RecognitionStatus.FACE_ACQUIRED -> "FACE LOCKED // SCANNING"
                        RecognitionStatus.LOST -> "REALIGN IN RETICLE"
                        RecognitionStatus.SEARCHING -> "SEARCHING // POSITION FACE"
                    }
                } else {
                    when (scanState) {
                        ScanState.IDLE -> "STATUS: STANDBY"
                        ScanState.SCANNING -> "ACQUIRING [${(scanProgress * 100).toInt()}%]"
                        ScanState.SUCCESS -> "SIMULATION: VERIFIED"
                        ScanState.RETRY -> "SIMULATION: REALIGN"
                        ScanState.ERROR -> "SIMULATION: UNMATCHED"
                    }
                }

                val stateColor = when (scanState) {
                    ScanState.SUCCESS -> AuraSuccess
                    ScanState.RETRY -> AuraWarning
                    ScanState.ERROR -> AuraError
                    ScanState.SCANNING -> accentColor
                    ScanState.IDLE -> AuraTextMuted
                }

                Text(
                    text = stateLabel,
                    color = stateColor,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                // Bottom Subtext
                Text(
                    text = if (backgroundMode == BackgroundMode.CAMERA && hasCameraPermission) {
                        if (realtimeFaceData.isFaceDetected) {
                            "REALTIME AI: YAW ${realtimeFaceData.headEulerY.toInt()}° ROLL ${realtimeFaceData.headEulerZ.toInt()}°"
                        } else {
                            "FEED: FRONT OPTICAL // SCANNING..."
                        }
                    } else {
                        "FEED: BIOMETRIC VECTOR MESH"
                    },
                    color = if (realtimeFaceData.isFaceDetected) AuraElectricAqua else AuraTextMuted,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                Text(
                    text = if (realtimeFaceData.isFaceDetected) "ML KIT // ON-DEVICE" else "60 FPS // GPU",
                    color = AuraTextMuted,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Drawing Helpers
// -------------------------------------------------------------

private fun DrawScope.drawBiometricSilhouette(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    accentColor: Color,
    glowIntensity: Float,
    scanLineWidth: Float,
    presetId: String,
    pulse: Float,
    scanY: Float,
    faceW: Float = w * 0.44f,
    faceH: Float = h * 0.48f,
    liveLeftEye: Offset? = null,
    liveRightEye: Offset? = null,
    liveNose: Offset? = null,
    liveMouth: Offset? = null
) {
    // Head Oval Path
    val headPath = Path().apply {
        moveTo(cx, cy - faceH * 0.85f)
        // Right cranium to cheek
        cubicTo(
            cx + faceW * 0.82f, cy - faceH * 0.80f,
            cx + faceW * 0.90f, cy + faceH * 0.10f,
            cx + faceW * 0.55f, cy + faceH * 0.70f
        )
        // Jaw to chin
        cubicTo(
            cx + faceW * 0.35f, cy + faceH * 0.95f,
            cx + faceW * 0.15f, cy + faceH * 1.05f,
            cx, cy + faceH * 1.05f
        )
        // Left chin to jaw
        cubicTo(
            cx - faceW * 0.15f, cy + faceH * 1.05f,
            cx - faceW * 0.35f, cy + faceH * 0.95f,
            cx - faceW * 0.55f, cy + faceH * 0.70f
        )
        // Left cheek to cranium
        cubicTo(
            cx - faceW * 0.90f, cy + faceH * 0.10f,
            cx - faceW * 0.82f, cy - faceH * 0.80f,
            cx, cy - faceH * 0.85f
        )
        close()
    }

    // Outer glow
    drawPath(
        path = headPath,
        color = accentColor.copy(alpha = 0.12f * glowIntensity),
        style = Stroke(width = scanLineWidth * 2.8f)
    )

    // Main stroke
    drawPath(
        path = headPath,
        color = accentColor.copy(alpha = 0.45f + 0.25f * pulse),
        style = Stroke(
            width = scanLineWidth,
            pathEffect = if (presetId == "minimal_orbit") PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
        )
    )

    // Biometric Landmark Nodes (Uses live detected eye/nose/mouth coordinates when available!)
    val eyeY = cy - faceH * 0.12f
    val eyeSpacing = faceW * 0.42f
    val noseY = cy + faceH * 0.22f
    val mouthY = cy + faceH * 0.55f

    val leftEye = liveLeftEye ?: Offset(cx - eyeSpacing, eyeY)
    val rightEye = liveRightEye ?: Offset(cx + eyeSpacing, eyeY)
    val noseTip = liveNose ?: Offset(cx, noseY)
    val mouthCenter = liveMouth ?: Offset(cx, mouthY)

    val landmarks = listOf(
        leftEye, // 0: Left eye
        rightEye, // 1: Right eye
        Offset(leftEye.x, leftEye.y - faceH * 0.18f), // 2: Left brow
        Offset(rightEye.x, rightEye.y - faceH * 0.18f), // 3: Right brow
        Offset(cx, cy), // 4: Nose bridge
        noseTip, // 5: Nose tip
        Offset(cx - faceW * 0.25f, noseTip.y + faceH * 0.05f), // 6: Left nostril
        Offset(cx + faceW * 0.25f, noseTip.y + faceH * 0.05f), // 7: Right nostril
        mouthCenter, // 8: Mouth center
        Offset(cx - faceW * 0.30f, mouthCenter.y), // 9: Mouth left
        Offset(cx + faceW * 0.30f, mouthCenter.y), // 10: Mouth right
        Offset(cx, mouthCenter.y + faceH * 0.15f), // 11: Lower lip
        Offset(cx, cy + faceH * 0.95f), // 12: Chin anchor
        Offset(cx - faceW * 0.65f, cy + faceH * 0.35f), // 13: Left cheek
        Offset(cx + faceW * 0.65f, cy + faceH * 0.35f), // 14: Right cheek
        Offset(cx - faceW * 0.6f, cy - faceH * 0.5f), // 15: Left temple
        Offset(cx + faceW * 0.6f, cy - faceH * 0.5f)  // 16: Right temple
    )

    // Draw triangulation lines connecting biometric landmarks
    val connections = listOf(
        0 to 1, 0 to 4, 1 to 4, 0 to 2, 1 to 3,
        4 to 5, 5 to 6, 5 to 7, 5 to 8, 8 to 9, 8 to 10,
        8 to 11, 11 to 12, 6 to 13, 7 to 14, 13 to 12, 14 to 12,
        2 to 15, 3 to 16, 15 to 0, 16 to 1
    )

    connections.forEach { (i1, i2) ->
        val p1 = landmarks[i1]
        val p2 = landmarks[i2]
        // Highlight triangulation lines when the scan plane is near them
        val distToScan = kotlin.math.abs((p1.y + p2.y) / 2f - scanY)
        val proximityBoost = (1f - (distToScan / (h * 0.2f)).coerceIn(0f, 1f))
        val lineAlpha = (0.12f + 0.35f * proximityBoost * glowIntensity).coerceIn(0f, 1f)

        drawLine(
            color = accentColor.copy(alpha = lineAlpha),
            start = p1,
            end = p2,
            strokeWidth = 1f + proximityBoost * 1.5f
        )
    }

    // Draw landmark node dots & reticles
    landmarks.forEachIndexed { idx, point ->
        val distToScan = kotlin.math.abs(point.y - scanY)
        val proximity = (1f - (distToScan / (h * 0.15f)).coerceIn(0f, 1f))

        val nodeRadius = (2.2f + proximity * 3.5f)
        val nodeAlpha = (0.35f + proximity * 0.65f).coerceIn(0f, 1f)

        // Outer glow on active node
        if (proximity > 0.3f) {
            drawCircle(
                color = accentColor.copy(alpha = 0.25f * proximity * glowIntensity),
                radius = nodeRadius * 2.8f,
                center = point
            )
        }

        drawCircle(
            color = if (proximity > 0.6f) Color.White else accentColor.copy(alpha = nodeAlpha),
            radius = nodeRadius,
            center = point
        )

        // Draw crosshair on eye nodes (idx 0 and 1)
        if (idx == 0 || idx == 1) {
            val crossSize = 10f + pulse * 4f
            drawLine(
                color = accentColor.copy(alpha = 0.5f),
                start = Offset(point.x - crossSize, point.y),
                end = Offset(point.x + crossSize, point.y),
                strokeWidth = 1f
            )
            drawLine(
                color = accentColor.copy(alpha = 0.5f),
                start = Offset(point.x, point.y - crossSize),
                end = Offset(point.x, point.y + crossSize),
                strokeWidth = 1f
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = crossSize * 0.85f,
                center = point,
                style = Stroke(width = 1f)
            )
        }
    }
}

// -------------------------------------------------------------
// Preset Specific Animation Effects
// -------------------------------------------------------------

private fun DrawScope.drawAquaPulseEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, pulse: Float
) {
    // 1. Concentric Sonar Pulses from center
    for (i in 1..3) {
        val r = (w * 0.18f * i + pulse * 45f) % (w * 0.55f)
        val alpha = (1f - (r / (w * 0.55f))).coerceIn(0f, 1f) * 0.35f * glowIntensity
        drawCircle(
            color = accentColor.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), pulse * 20f))
        )
    }

    // 2. High-intensity aqua laser scanline with vertical fade trail
    val trailH = h * 0.12f
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0f),
            accentColor.copy(alpha = 0.2f * glowIntensity),
            accentColor.copy(alpha = 0.85f * glowIntensity)
        ),
        startY = scanY - trailH,
        endY = scanY
    )
    drawRect(
        brush = gradientBrush,
        topLeft = Offset(w * 0.1f, scanY - trailH),
        size = Size(w * 0.8f, trailH)
    )

    // Solid core beam
    drawLine(
        color = Color.White.copy(alpha = 0.95f),
        start = Offset(w * 0.1f, scanY),
        end = Offset(w * 0.9f, scanY),
        strokeWidth = scanLineWidth,
        cap = StrokeCap.Round
    )

    // Soft outer glow beam
    drawLine(
        color = accentColor.copy(alpha = 0.6f * glowIntensity),
        start = Offset(w * 0.08f, scanY),
        end = Offset(w * 0.92f, scanY),
        strokeWidth = scanLineWidth * 3.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawNeuralScanEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, pulse: Float
) {
    // Pulsing neural energy rings
    val synapticColor = AuraNeonViolet
    for (i in 0 until 6) {
        val angle = (i * 60f + pulse * 360f) * (PI / 180f).toFloat()
        val radius = w * 0.28f + sin(pulse * 6f + i) * 16f
        val px = cx + cos(angle) * radius
        val py = cy + sin(angle) * radius

        drawLine(
            color = synapticColor.copy(alpha = 0.4f * glowIntensity),
            start = Offset(cx, cy),
            end = Offset(px, py),
            strokeWidth = 1.2f
        )
        drawCircle(
            color = accentColor,
            radius = 3f + pulse * 2.5f,
            center = Offset(px, py)
        )
    }

    // Neural scan wave
    drawLine(
        color = accentColor.copy(alpha = 0.9f),
        start = Offset(w * 0.15f, scanY),
        end = Offset(w * 0.85f, scanY),
        strokeWidth = scanLineWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHolographicEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, pulse: Float
) {
    // Horizontal topographical slicing lines
    val sliceCount = 14
    val step = (h * 0.65f) / sliceCount
    val startY = h * 0.18f

    for (i in 0 until sliceCount) {
        val y = startY + i * step
        val waveOffset = sin((y + pulse * 200f) * 0.05f) * 12f
        val distToScan = kotlin.math.abs(y - scanY)
        val active = (1f - (distToScan / (h * 0.12f)).coerceIn(0f, 1f))
        val alpha = (0.12f + active * 0.55f) * glowIntensity

        // Chromatic split
        drawLine(
            color = AuraNeonViolet.copy(alpha = alpha * 0.4f),
            start = Offset(w * 0.22f + waveOffset - 3f, y),
            end = Offset(w * 0.78f + waveOffset - 3f, y),
            strokeWidth = 1f
        )
        drawLine(
            color = AuraElectricAqua.copy(alpha = alpha),
            start = Offset(w * 0.22f + waveOffset, y),
            end = Offset(w * 0.78f + waveOffset, y),
            strokeWidth = 1.2f + active * 1.5f
        )
    }
}

private fun DrawScope.drawCyberGridEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, rotAngle: Float
) {
    // Isometric grid lines
    val gridSize = 32f
    val startX = w * 0.15f
    val endX = w * 0.85f
    val startY = h * 0.15f
    val endY = h * 0.85f

    // Vertical lines
    var x = startX
    while (x <= endX) {
        drawLine(
            color = accentColor.copy(alpha = 0.08f * glowIntensity),
            start = Offset(x, startY),
            end = Offset(x, endY),
            strokeWidth = 0.8f
        )
        x += gridSize
    }

    // Horizontal lines
    var y = startY
    while (y <= endY) {
        val dist = kotlin.math.abs(y - scanY)
        val alpha = if (dist < 40f) 0.55f else 0.08f
        drawLine(
            color = accentColor.copy(alpha = alpha * glowIntensity),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = if (dist < 40f) scanLineWidth else 0.8f
        )
        y += gridSize
    }

    // Laser crosshair ticks
    val tickSize = 14f
    drawLine(
        color = Color.White,
        start = Offset(cx - tickSize, scanY),
        end = Offset(cx + tickSize, scanY),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.White,
        start = Offset(cx, scanY - tickSize),
        end = Offset(cx, scanY + tickSize),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawMinimalOrbitEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, rotAngle: Float
) {
    // Outer dashed ring
    drawCircle(
        color = accentColor.copy(alpha = 0.35f * glowIntensity),
        radius = w * 0.38f,
        center = Offset(cx, cy),
        style = Stroke(
            width = scanLineWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 18f), rotAngle * 2f)
        )
    )

    // Inner counter-rotating ring
    drawCircle(
        color = accentColor.copy(alpha = 0.45f * glowIntensity),
        radius = w * 0.32f,
        center = Offset(cx, cy),
        style = Stroke(
            width = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 22f), -rotAngle * 3f)
        )
    )

    // Sleek single scanning bar
    drawLine(
        color = accentColor.copy(alpha = 0.8f),
        start = Offset(cx - w * 0.32f, scanY),
        end = Offset(cx + w * 0.32f, scanY),
        strokeWidth = scanLineWidth,
        cap = StrokeCap.Square
    )
}

private fun DrawScope.drawNeonContourEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, pulse: Float
) {
    // Vibrant neon beam with multi-layer glow
    val beamW = w * 0.82f
    val startX = (w - beamW) / 2f
    val endX = startX + beamW

    // Outermost halo
    drawLine(
        color = accentColor.copy(alpha = 0.25f * glowIntensity),
        start = Offset(startX - 10f, scanY),
        end = Offset(endX + 10f, scanY),
        strokeWidth = scanLineWidth * 5f,
        cap = StrokeCap.Round
    )

    // Inner vibrant core
    drawLine(
        color = accentColor.copy(alpha = 0.85f * glowIntensity),
        start = Offset(startX, scanY),
        end = Offset(endX, scanY),
        strokeWidth = scanLineWidth * 2f,
        cap = StrokeCap.Round
    )

    // White hot center
    drawLine(
        color = Color.White.copy(alpha = 0.95f),
        start = Offset(startX + 15f, scanY),
        end = Offset(endX - 15f, scanY),
        strokeWidth = scanLineWidth * 0.7f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDigitalParticlesEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, particles: List<SimParticle>, pulse: Float
) {
    // Render upward cyber dust particles
    particles.forEach { p ->
        // Drift upwards
        p.yRatio = (p.yRatio - p.speed + 1f) % 1f
        val px = p.xRatio * w
        val py = p.yRatio * h

        val distToScan = kotlin.math.abs(py - scanY)
        val nearScan = distToScan < 30f
        val alpha = if (nearScan) (0.9f * glowIntensity) else (0.25f * glowIntensity)

        drawCircle(
            color = if (nearScan) Color.White else accentColor.copy(alpha = alpha),
            radius = if (nearScan) p.radius * 1.8f else p.radius,
            center = Offset(px, py)
        )
    }

    // Scanning bar
    drawLine(
        color = accentColor.copy(alpha = 0.7f),
        start = Offset(w * 0.15f, scanY),
        end = Offset(w * 0.85f, scanY),
        strokeWidth = scanLineWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )
}

private fun DrawScope.drawAuroraScanEffect(
    cx: Float, cy: Float, w: Float, h: Float,
    scanY: Float, accentColor: Color, glowIntensity: Float,
    scanLineWidth: Float, pulse: Float
) {
    // Diagonal flowing aurora wave
    val auroraBrush = Brush.linearGradient(
        colors = listOf(
            AuraElectricAqua.copy(alpha = 0.15f * glowIntensity),
            AuraNeonViolet.copy(alpha = 0.35f * glowIntensity),
            AuraCyberCyan.copy(alpha = 0.25f * glowIntensity),
            Color.Transparent
        ),
        start = Offset(w * 0.1f, scanY - h * 0.15f),
        end = Offset(w * 0.9f, scanY + h * 0.15f)
    )

    drawRect(
        brush = auroraBrush,
        topLeft = Offset(w * 0.1f, scanY - h * 0.12f),
        size = Size(w * 0.8f, h * 0.24f)
    )

    drawLine(
        color = AuraElectricAqua.copy(alpha = 0.9f),
        start = Offset(w * 0.12f, scanY),
        end = Offset(w * 0.88f, scanY),
        strokeWidth = scanLineWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCyberHUDFrame(
    w: Float,
    h: Float,
    accentColor: Color,
    lineWidth: Float,
    glowIntensity: Float,
    scanState: ScanState,
    scanProgress: Float,
    faceBounds: RectF? = null
) {
    val cornerLen = 28f
    val pad = 24f

    // 1. Live Face Target Box tracking the detected head
    if (faceBounds != null) {
        val bLeft = faceBounds.left * w
        val bTop = faceBounds.top * h
        val bRight = faceBounds.right * w
        val bBottom = faceBounds.bottom * h
        val tLen = 20f
        val reticleWidth = lineWidth * 1.4f

        // Top-Left Face Bracket
        drawLine(color = accentColor, start = Offset(bLeft, bTop), end = Offset(bLeft + tLen, bTop), strokeWidth = reticleWidth)
        drawLine(color = accentColor, start = Offset(bLeft, bTop), end = Offset(bLeft, bTop + tLen), strokeWidth = reticleWidth)

        // Top-Right Face Bracket
        drawLine(color = accentColor, start = Offset(bRight, bTop), end = Offset(bRight - tLen, bTop), strokeWidth = reticleWidth)
        drawLine(color = accentColor, start = Offset(bRight, bTop), end = Offset(bRight, bTop + tLen), strokeWidth = reticleWidth)

        // Bottom-Left Face Bracket
        drawLine(color = accentColor, start = Offset(bLeft, bBottom), end = Offset(bLeft + tLen, bBottom), strokeWidth = reticleWidth)
        drawLine(color = accentColor, start = Offset(bLeft, bBottom), end = Offset(bLeft, bBottom - tLen), strokeWidth = reticleWidth)

        // Bottom-Right Face Bracket
        drawLine(color = accentColor, start = Offset(bRight, bBottom), end = Offset(bRight - tLen, bBottom), strokeWidth = reticleWidth)
        drawLine(color = accentColor, start = Offset(bRight, bBottom), end = Offset(bRight, bBottom - tLen), strokeWidth = reticleWidth)

        // Bounding box subtle glow outline
        drawRect(
            color = accentColor.copy(alpha = 0.08f * glowIntensity),
            topLeft = Offset(bLeft, bTop),
            size = Size(bRight - bLeft, bBottom - bTop)
        )
    }

    // 2. 4 Outer Corner Brackets
    // Top-Left
    drawLine(color = accentColor, start = Offset(pad, pad), end = Offset(pad + cornerLen, pad), strokeWidth = lineWidth)
    drawLine(color = accentColor, start = Offset(pad, pad), end = Offset(pad, pad + cornerLen), strokeWidth = lineWidth)

    // Top-Right
    drawLine(color = accentColor, start = Offset(w - pad, pad), end = Offset(w - pad - cornerLen, pad), strokeWidth = lineWidth)
    drawLine(color = accentColor, start = Offset(w - pad, pad), end = Offset(w - pad, pad + cornerLen), strokeWidth = lineWidth)

    // Bottom-Left
    drawLine(color = accentColor, start = Offset(pad, h - pad), end = Offset(pad + cornerLen, h - pad), strokeWidth = lineWidth)
    drawLine(color = accentColor, start = Offset(pad, h - pad), end = Offset(pad, h - pad - cornerLen), strokeWidth = lineWidth)

    // Bottom-Right
    drawLine(color = accentColor, start = Offset(w - pad, h - pad), end = Offset(w - pad - cornerLen, h - pad), strokeWidth = lineWidth)
    drawLine(color = accentColor, start = Offset(w - pad, h - pad), end = Offset(w - pad, pad + cornerLen), strokeWidth = lineWidth)

    // Progress Arc indicator if scanning
    if (scanState == ScanState.SCANNING && scanProgress > 0f) {
        val sweepAngle = 360f * scanProgress
        drawArc(
            color = accentColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(pad * 1.5f, pad * 1.5f),
            size = Size(w - pad * 3f, h - pad * 3f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }

    // Success Burst Ring
    if (scanState == ScanState.SUCCESS) {
        drawCircle(
            color = AuraSuccess.copy(alpha = 0.3f),
            radius = w * 0.42f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = 3f)
        )
    }
}
