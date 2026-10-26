package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ScanState
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraWarning
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun DynamicIslandBar(
    viewModel: AuraUnlockViewModel,
    modifier: Modifier = Modifier
) {
    val scanState by viewModel.scanState.collectAsState()
    val isEnabled by viewModel.isDynamicIslandEnabled.collectAsState()

    if (!isEnabled) return

    val isScanningOrSuccess = scanState != ScanState.IDLE

    // Dynamic Island is always present at top center!
    // Resting: elongated 160x36dp iPhone-style camera notch capsule with luminous light rim
    // Active Face ID: expands with bouncy iOS spring physics to 84x84dp square
    val islandWidth by animateDpAsState(
        targetValue = if (isScanningOrSuccess) 84.dp else 160.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IslandWidth"
    )

    val islandHeight by animateDpAsState(
        targetValue = if (isScanningOrSuccess) 84.dp else 36.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IslandHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isScanningOrSuccess) 24.dp else 18.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IslandCorner"
    )

    // Luminous light rim gradient along the length of the capsule
    val rimLightBrush = remember(scanState) {
        when (scanState) {
            ScanState.SUCCESS -> Brush.horizontalGradient(
                listOf(AuraSuccess.copy(alpha = 0.5f), AuraSuccess, AuraSuccess.copy(alpha = 0.5f))
            )
            ScanState.SCANNING -> Brush.horizontalGradient(
                listOf(AuraElectricAqua.copy(alpha = 0.4f), Color.White, AuraElectricAqua.copy(alpha = 0.4f))
            )
            else -> Brush.horizontalGradient(
                listOf(
                    Color(0xFF1E2634),
                    Color(0x8035F5E5), // soft aqua light rim running through center
                    Color(0x40FFFFFF), // subtle glass reflection
                    Color(0xFF1E2634)
                )
            )
        }
    }

    Box(
        modifier = modifier.testTag("dynamic_island_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .shadow(
                    elevation = if (isScanningOrSuccess) 18.dp else 8.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = if (scanState == ScanState.SUCCESS) AuraSuccess else AuraElectricAqua,
                    spotColor = if (scanState == ScanState.SUCCESS) AuraSuccess else AuraElectricAqua
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF000000))
                .border(
                    width = if (scanState == ScanState.SUCCESS) 1.5.dp else 1.1.dp,
                    brush = rimLightBrush,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tapping the resting pill triggers the face scan simulation
                    if (scanState == ScanState.IDLE) {
                        viewModel.startScanSimulation()
                    }
                }
                .padding(if (isScanningOrSuccess) 12.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isScanningOrSuccess,
                transitionSpec = {
                    fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(140))
                },
                label = "IslandContentMode"
            ) { active ->
                if (active) {
                    // Face ID Square Scanning Animation with tick morph
                    IPhoneFaceIDSquareCanvas(scanState = scanState)
                } else {
                    // Resting Camera Notch Capsule
                    RestingIslandPill()
                }
            }
        }
    }
}

@Composable
private fun RestingIslandPill() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Minimalist Face ID wireframe glyph
        Canvas(modifier = Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height
            val color = Color(0xFFE2E8F0)

            // Mini corner ticks
            val b = 4f
            drawLine(color, Offset(0f, 0f), Offset(b, 0f), 1.4f)
            drawLine(color, Offset(0f, 0f), Offset(0f, b), 1.4f)
            drawLine(color, Offset(w, 0f), Offset(w - b, 0f), 1.4f)
            drawLine(color, Offset(w, 0f), Offset(w, b), 1.4f)
            drawLine(color, Offset(0f, h), Offset(b, h), 1.4f)
            drawLine(color, Offset(0f, h), Offset(0f, h - b), 1.4f)
            drawLine(color, Offset(w, h), Offset(w - b, h), 1.4f)
            drawLine(color, Offset(w, h), Offset(w, h - b), 1.4f)

            // Eyes
            drawCircle(color, radius = 1.2f, center = Offset(w * 0.36f, h * 0.40f))
            drawCircle(color, radius = 1.2f, center = Offset(w * 0.64f, h * 0.40f))

            // Smile
            val smile = Path().apply {
                moveTo(w * 0.34f, h * 0.68f)
                quadraticTo(w * 0.5f, h * 0.82f, w * 0.66f, h * 0.68f)
            }
            drawPath(smile, color, style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        }

        // Center: Subtle micro light indicator along the capsule length
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x1A35F5E5), Color(0x6635F5E5), Color(0x1A35F5E5))
                    )
                )
        )

        // Right side: Camera cutout optics module
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF0C1017))
                .border(1.dp, Color(0xFF222C3D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A5F))
            )
        }
    }
}

@Composable
private fun IPhoneFaceIDSquareCanvas(
    scanState: ScanState
) {
    // Laser scan sweep phase
    val scanYAnim = remember { Animatable(0f) }
    // Tick checkmark reveal progress (0f to 1f)
    val checkmarkAnim = remember { Animatable(0f) }

    LaunchedEffect(scanState) {
        if (scanState == ScanState.SCANNING) {
            checkmarkAnim.snapTo(0f)
            scanYAnim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else if (scanState == ScanState.SUCCESS) {
            scanYAnim.stop()
            checkmarkAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            scanYAnim.snapTo(0f)
            checkmarkAnim.snapTo(0f)
        }
    }

    val faceAlpha by animateFloatAsState(
        targetValue = if (scanState == ScanState.SUCCESS) 0f else 1f,
        animationSpec = tween(180),
        label = "FaceAlpha"
    )

    Canvas(modifier = Modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        val pad = 4f

        val boxColor = when (scanState) {
            ScanState.SUCCESS -> AuraSuccess
            ScanState.RETRY -> AuraWarning
            ScanState.ERROR -> AuraError
            else -> Color(0xFFE2E8F0)
        }

        // 1. Classic iPhone Face ID Rounded Corner Brackets
        val cornerLen = 11f
        val strokeW = 2.4f

        // Top-Left corner
        drawLine(
            color = boxColor,
            start = Offset(pad, pad + cornerLen),
            end = Offset(pad, pad + 3f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawArc(
            color = boxColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = Size(6f, 6f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawLine(
            color = boxColor,
            start = Offset(pad + 3f, pad),
            end = Offset(pad + cornerLen, pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Top-Right corner
        drawLine(
            color = boxColor,
            start = Offset(w - pad - cornerLen, pad),
            end = Offset(w - pad - 3f, pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawArc(
            color = boxColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w - pad - 6f, pad),
            size = Size(6f, 6f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawLine(
            color = boxColor,
            start = Offset(w - pad, pad + 3f),
            end = Offset(w - pad, pad + cornerLen),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Bottom-Left corner
        drawLine(
            color = boxColor,
            start = Offset(pad, h - pad - cornerLen),
            end = Offset(pad, h - pad - 3f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawArc(
            color = boxColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(pad, h - pad - 6f),
            size = Size(6f, 6f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawLine(
            color = boxColor,
            start = Offset(pad + 3f, h - pad),
            end = Offset(pad + cornerLen, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Bottom-Right corner
        drawLine(
            color = boxColor,
            start = Offset(w - pad - cornerLen, h - pad),
            end = Offset(w - pad - 3f, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawArc(
            color = boxColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w - pad - 6f, h - pad - 6f),
            size = Size(6f, 6f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawLine(
            color = boxColor,
            start = Offset(w - pad, h - pad - cornerLen),
            end = Offset(w - pad, h - pad - 3f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // 2. Face Silhouette Inside (Fades out on SUCCESS)
        if (faceAlpha > 0.05f) {
            val faceColor = Color.White.copy(alpha = faceAlpha * 0.95f)

            // Eyes
            drawCircle(faceColor, radius = 2.2f, center = Offset(w * 0.36f, h * 0.38f))
            drawCircle(faceColor, radius = 2.2f, center = Offset(w * 0.64f, h * 0.38f))

            // Nose tick
            drawLine(
                color = faceColor,
                start = Offset(w * 0.50f, h * 0.44f),
                end = Offset(w * 0.50f, h * 0.56f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // Smile Arc
            val smilePath = Path().apply {
                moveTo(w * 0.34f, h * 0.68f)
                quadraticTo(w * 0.50f, h * 0.80f, w * 0.66f, h * 0.68f)
            }
            drawPath(
                path = smilePath,
                color = faceColor,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Sweeping laser scanline inside the square
            if (scanState == ScanState.SCANNING) {
                val scanLineY = pad + 6f + scanYAnim.value * (h - pad * 2f - 12f)
                drawLine(
                    color = AuraElectricAqua.copy(alpha = 0.45f * faceAlpha),
                    start = Offset(pad + 4f, scanLineY),
                    end = Offset(w - pad - 4f, scanLineY),
                    strokeWidth = 4.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.95f * faceAlpha),
                    start = Offset(pad + 5f, scanLineY),
                    end = Offset(w - pad - 5f, scanLineY),
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Iconic Animated Green Checkmark / Tick Symbol (Draws on SUCCESS)
        if (checkmarkAnim.value > 0.01f) {
            val fullTickPath = Path().apply {
                moveTo(w * 0.28f, h * 0.52f)
                lineTo(w * 0.46f, h * 0.70f)
                lineTo(w * 0.74f, h * 0.32f)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(fullTickPath, false)
            val pathLength = pathMeasure.length

            val animatedTickPath = Path()
            pathMeasure.getSegment(0f, pathLength * checkmarkAnim.value, animatedTickPath, true)

            // Glow backing
            drawPath(
                path = animatedTickPath,
                color = AuraSuccess.copy(alpha = 0.35f),
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Crisp green tick stroke
            drawPath(
                path = animatedTickPath,
                color = AuraSuccess,
                style = Stroke(width = 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
