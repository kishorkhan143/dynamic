package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraSuccess

class DynamicIslandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        const val CHANNEL_ID = "dynamic_island_overlay_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, DynamicIslandOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DynamicIslandOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Settings.canDrawOverlays(this)) {
            showFloatingDynamicIsland()
        }
    }

    private fun showFloatingDynamicIsland() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setContent {
                FloatingSystemPill(
                    onOpenApp = {
                        val launchIntent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(launchIntent)
                    }
                )
            }
        }

        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Dynamic Island Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Dynamic Island floating near your camera cutout"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aura Dynamic Island Active")
            .setContentText("Tap to open Aura Unlock face scanner")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
}

@Composable
private fun FloatingSystemPill(
    onOpenApp: () -> Unit
) {
    var scanPhase by remember { mutableStateOf(0) } // 0: resting, 1: scanning, 2: success

    val isExpanded = scanPhase != 0

    val widthAnim by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isExpanded) 84.dp else 160.dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "FloatingWidth"
    )

    val heightAnim by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isExpanded) 84.dp else 36.dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "FloatingHeight"
    )

    val cornerAnim by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 18.dp,
        label = "FloatingCorner"
    )

    androidx.compose.runtime.LaunchedEffect(scanPhase) {
        if (scanPhase == 1) {
            kotlinx.coroutines.delay(1000)
            scanPhase = 2
            kotlinx.coroutines.delay(700)
            onOpenApp()
            scanPhase = 0
        }
    }

    val rimLightBrush = Brush.horizontalGradient(
        when (scanPhase) {
            2 -> listOf(AuraSuccess.copy(alpha = 0.5f), AuraSuccess, AuraSuccess.copy(alpha = 0.5f))
            1 -> listOf(AuraElectricAqua.copy(alpha = 0.4f), Color.White, AuraElectricAqua.copy(alpha = 0.4f))
            else -> listOf(
                Color(0xFF1E2634),
                Color(0x8035F5E5),
                Color(0x40FFFFFF),
                Color(0xFF1E2634)
            )
        }
    )

    Box(
        modifier = Modifier
            .width(widthAnim)
            .height(heightAnim)
            .clip(RoundedCornerShape(cornerAnim))
            .background(Color(0xFF000000))
            .border(1.2.dp, rimLightBrush, RoundedCornerShape(cornerAnim))
            .clickable {
                if (scanPhase == 0) {
                    scanPhase = 1
                }
            }
            .padding(if (isExpanded) 10.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Face ID icon
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val color = Color(0xFFE2E8F0)
                    drawCircle(color, radius = 1.2f, center = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.4f))
                    drawCircle(color, radius = 1.2f, center = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.4f))
                    val smile = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.35f, h * 0.7f)
                        quadraticTo(w * 0.5f, h * 0.85f, w * 0.65f, h * 0.7f)
                    }
                    drawPath(smile, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                }

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

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C1017))
                        .border(1.dp, Color(0xFF222C3D), CircleShape)
                )
            }
        } else {
            // Face ID square scanning HUD
            androidx.compose.foundation.Canvas(modifier = Modifier.size(54.dp)) {
                val w = size.width
                val h = size.height
                val color = if (scanPhase == 2) AuraSuccess else AuraElectricAqua

                // Corner brackets
                val b = 10f
                val sw = 2.2f
                drawLine(color, androidx.compose.ui.geometry.Offset(4f, 4f + b), androidx.compose.ui.geometry.Offset(4f, 4f), sw)
                drawLine(color, androidx.compose.ui.geometry.Offset(4f, 4f), androidx.compose.ui.geometry.Offset(4f + b, 4f), sw)

                drawLine(color, androidx.compose.ui.geometry.Offset(w - 4f - b, 4f), androidx.compose.ui.geometry.Offset(w - 4f, 4f), sw)
                drawLine(color, androidx.compose.ui.geometry.Offset(w - 4f, 4f), androidx.compose.ui.geometry.Offset(w - 4f, 4f + b), sw)

                drawLine(color, androidx.compose.ui.geometry.Offset(4f, h - 4f - b), androidx.compose.ui.geometry.Offset(4f, h - 4f), sw)
                drawLine(color, androidx.compose.ui.geometry.Offset(4f, h - 4f), androidx.compose.ui.geometry.Offset(4f + b, h - 4f), sw)

                drawLine(color, androidx.compose.ui.geometry.Offset(w - 4f - b, h - 4f), androidx.compose.ui.geometry.Offset(w - 4f, h - 4f), sw)
                drawLine(color, androidx.compose.ui.geometry.Offset(w - 4f, h - 4f), androidx.compose.ui.geometry.Offset(w - 4f, h - 4f - b), sw)

                if (scanPhase == 2) {
                    // Checkmark
                    val tick = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.28f, h * 0.52f)
                        lineTo(w * 0.46f, h * 0.70f)
                        lineTo(w * 0.74f, h * 0.32f)
                    }
                    drawPath(tick, AuraSuccess, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                } else {
                    // Face silhouette + scanning line
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = 2f, center = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.38f))
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = 2f, center = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.38f))
                    drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.44f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.56f), 1.8f)
                    val smile = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.35f, h * 0.68f)
                        quadraticTo(w * 0.5f, h * 0.80f, w * 0.65f, h * 0.68f)
                    }
                    drawPath(smile, Color.White.copy(alpha = 0.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f))
                    // Sweeping laser
                    drawLine(AuraElectricAqua, androidx.compose.ui.geometry.Offset(8f, h * 0.5f), androidx.compose.ui.geometry.Offset(w - 8f, h * 0.5f), 2.5f)
                }
            }
        }
    }
}
