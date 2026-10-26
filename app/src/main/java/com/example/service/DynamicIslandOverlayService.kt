package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class SystemOverlayPhase {
    HIDDEN,
    RESTING,
    SCANNING,
    SUCCESS,
    COLLAPSED
}

class DynamicIslandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var animationJob: Job? = null
    private val overlayPhaseState = MutableStateFlow(SystemOverlayPhase.RESTING)
    private var orientationListener: OrientationEventListener? = null

    companion object {
        const val CHANNEL_ID = "dynamic_island_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TRIGGER_APP_OPEN = "com.example.action.TRIGGER_APP_OPEN"

        var isRunning: Boolean = false
            private set

        fun startService(context: Context, triggerAppOpen: Boolean = false) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, DynamicIslandOverlayService::class.java).apply {
                    if (triggerAppOpen) {
                        action = ACTION_TRIGGER_APP_OPEN
                    }
                }
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            showFloatingDynamicIsland()
        }
        if (intent?.action == ACTION_TRIGGER_APP_OPEN) {
            playOverlayAnimation()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Ensure the overlay service stays running or restarts immediately if the task is removed from Recents
        if (Settings.canDrawOverlays(this)) {
            val restartIntent = Intent(applicationContext, DynamicIslandOverlayService::class.java).also {
                it.setPackage(packageName)
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                1002,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 500L,
                pendingIntent
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Monitor orientation changes across any running application to keep Dynamic Island pinned and visible
        orientationListener = object : OrientationEventListener(this) {
            private var lastRotation = -1
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val wm = windowManager ?: return
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    wm.defaultDisplay
                }
                val currentRotation = display?.rotation ?: return
                if (currentRotation != lastRotation) {
                    lastRotation = currentRotation
                    repositionOverlay()
                }
            }
        }
        if (orientationListener?.canDetectOrientation() == true) {
            orientationListener?.enable()
        }

        if (Settings.canDrawOverlays(this)) {
            showFloatingDynamicIsland()
        }

        // Dynamically update position Y (top offset) in real-time
        serviceScope.launch {
            OverlayTrigger.islandOffsetY.collect { offsetY ->
                val view = overlayView ?: return@collect
                val wm = windowManager ?: return@collect
                val p = view.layoutParams as? WindowManager.LayoutParams ?: return@collect
                val density = resources.displayMetrics.density
                val newY = (offsetY * density).toInt()
                if (p.y != newY) {
                    p.y = newY
                    try {
                        wm.updateViewLayout(view, p)
                    } catch (_: Exception) {}
                }
            }
        }

        // Dynamically update position X (horizontal offset) in real-time
        serviceScope.launch {
            OverlayTrigger.islandOffsetX.collect { offsetX ->
                val view = overlayView ?: return@collect
                val wm = windowManager ?: return@collect
                val p = view.layoutParams as? WindowManager.LayoutParams ?: return@collect
                val density = resources.displayMetrics.density
                val newX = (offsetX * density).toInt()
                if (p.x != newX) {
                    p.x = newX
                    try {
                        wm.updateViewLayout(view, p)
                    } catch (_: Exception) {}
                }
            }
        }

        // Listen for triggers from device unlock, app open accessibility, or manual preview
        serviceScope.launch {
            OverlayTrigger.events.collect { _ ->
                playOverlayAnimation()
            }
        }
    }

    private fun playOverlayAnimation() {
        // Animation sequence driven dynamically by animationVelocity:
        // 1. Ensure capsule is resting at top-center camera apex
        // 2. Settle pause (e.g. 150ms / velocity)
        // 3. Expand to Face ID square with laser scanning (e.g. 1100ms / velocity)
        // 4. Show verified green checkmark (e.g. 1200ms / velocity)
        // 5. Spring-collapse back to capsule (e.g. 600ms / velocity)
        // 6. Return to persistent resting capsule at top camera center (turned into island)
        animationJob?.cancel()
        animationJob = serviceScope.launch {
            try {
                OverlayTrigger.setPlaying(true)
                overlayView?.visibility = View.VISIBLE

                val velocity = OverlayTrigger.animationVelocity.value.coerceIn(0.5f, 3.0f)
                val settleDelay = (150L / velocity).toLong().coerceAtLeast(30L)
                val scanDuration = (1100L / velocity).toLong().coerceAtLeast(250L)
                val successDuration = (1200L / velocity).toLong().coerceAtLeast(250L)
                val collapseDuration = (600L / velocity).toLong().coerceAtLeast(150L)

                overlayPhaseState.value = SystemOverlayPhase.RESTING
                delay(settleDelay)

                overlayPhaseState.value = SystemOverlayPhase.SCANNING
                delay(scanDuration)

                overlayPhaseState.value = SystemOverlayPhase.SUCCESS
                delay(successDuration)

                overlayPhaseState.value = SystemOverlayPhase.COLLAPSED
                delay(collapseDuration)

                // Smoothly remains as persistent resting island capsule at camera cutout
                overlayPhaseState.value = SystemOverlayPhase.RESTING
            } finally {
                OverlayTrigger.setPlaying(false)
            }
        }
    }

    private fun showFloatingDynamicIsland() {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val initialY = (OverlayTrigger.islandOffsetY.value * density).toInt()
        val initialX = (OverlayTrigger.islandOffsetX.value * density).toInt()

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
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = initialX
            y = initialY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@DynamicIslandOverlayService))
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setContent {
                val currentPhase by overlayPhaseState.collectAsState()
                val islandDarkness by OverlayTrigger.islandDarkness.collectAsState()
                val animationVelocity by OverlayTrigger.animationVelocity.collectAsState()
                val islandScale by OverlayTrigger.islandScale.collectAsState()
                val greenLightEnabled by OverlayTrigger.greenLightEnabled.collectAsState()

                SystemDynamicIslandOverlayContent(
                    phase = currentPhase,
                    darkness = islandDarkness,
                    velocity = animationVelocity,
                    scale = islandScale,
                    showGreenIndicator = greenLightEnabled
                )
            }
        }

        // Always visible at the top-center camera apex
        composeView.visibility = View.VISIBLE
        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Maintain active Dynamic Island overlay when rotating between portrait and landscape
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            showFloatingDynamicIsland()
        } else {
            repositionOverlay()
        }
    }

    private fun repositionOverlay() {
        val view = overlayView ?: return
        val wm = windowManager ?: return
        val p = view.layoutParams as? WindowManager.LayoutParams ?: return
        val density = resources.displayMetrics.density
        p.x = (OverlayTrigger.islandOffsetX.value * density).toInt()
        p.y = (OverlayTrigger.islandOffsetY.value * density).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            view.visibility = View.VISIBLE
            wm.updateViewLayout(view, p)
            view.requestLayout()
            view.invalidate()
        } catch (_: Exception) {}
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
            .setContentText("System-wide Face ID overlay active at camera notch")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        try {
            orientationListener?.disable()
        } catch (_: Exception) {}
        orientationListener = null

        serviceScope.cancel()

        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
}

/**
 * Computes island capsule background color from lightness/darkness setting.
 * 0.0f = Lighter frosted slate (R: 48, G: 58, B: 77, 92% alpha)
 * 1.0f = Pure Pitch Black OLED (R: 0, G: 0, B: 0, 100% alpha)
 */
fun getIslandBackgroundColor(darkness: Float): Color {
    val d = darkness.coerceIn(0f, 1f)
    if (d >= 0.85f) {
        return Color(0xFF000000)
    }
    val r = (36 * (1f - d)).toInt()
    val g = (44 * (1f - d)).toInt()
    val b = (58 * (1f - d)).toInt()
    val alpha = 0.90f + 0.10f * d
    return Color(r, g, b).copy(alpha = alpha)
}

@Composable
private fun SystemDynamicIslandOverlayContent(
    phase: SystemOverlayPhase,
    darkness: Float = 1.0f,
    velocity: Float = 1.0f,
    scale: Float = 1.0f,
    showGreenIndicator: Boolean = false
) {
    if (phase == SystemOverlayPhase.HIDDEN) {
        return
    }

    val isExpanded = phase == SystemOverlayPhase.SCANNING || phase == SystemOverlayPhase.SUCCESS
    val speedScale = velocity.coerceIn(0.5f, 3.0f)
    val s = scale.coerceIn(0.5f, 1.4f)

    val targetWidth = if (isExpanded) 84.dp * maxOf(s, 0.85f) else 160.dp * s
    val targetHeight = if (isExpanded) 84.dp * maxOf(s, 0.85f) else 36.dp * s
    val targetCorner = if (isExpanded) 24.dp * maxOf(s, 0.85f) else 18.dp * s

    val islandWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow * speedScale
        ),
        label = "SysIslandWidth"
    )

    val islandHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow * speedScale
        ),
        label = "SysIslandHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium * speedScale
        ),
        label = "SysIslandCorner"
    )

    val d = darkness.coerceIn(0f, 1f)
    val rimLightBrush = remember(phase, d) {
        when (phase) {
            SystemOverlayPhase.SUCCESS -> Brush.horizontalGradient(
                listOf(AuraSuccess.copy(alpha = 0.5f), AuraSuccess, AuraSuccess.copy(alpha = 0.5f))
            )
            SystemOverlayPhase.SCANNING -> Brush.horizontalGradient(
                listOf(AuraElectricAqua.copy(alpha = 0.5f), Color.White, AuraElectricAqua.copy(alpha = 0.5f))
            )
            else -> {
                // Resting state: pure stealth dark rim without persistent green/aqua halo
                if (d >= 0.70f) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF141720), Color(0xFF222936), Color(0xFF141720))
                    )
                } else if (d < 0.45f) {
                    Brush.horizontalGradient(
                        listOf(Color(0x60FFFFFF), Color(0x8035F5E5), Color(0x60FFFFFF))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1E2634), Color(0xFF2C3748), Color(0xFF1E2634))
                    )
                }
            }
        }
    }

    val backgroundColor = remember(d) { getIslandBackgroundColor(d) }

    // When resting, shadow is pure deep black - NEVER glowing green or aqua
    val shadowColor = when (phase) {
        SystemOverlayPhase.SUCCESS -> AuraSuccess
        SystemOverlayPhase.SCANNING -> AuraElectricAqua
        else -> Color.Black
    }

    Box(
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .shadow(
                    elevation = if (isExpanded) 18.dp else 6.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(backgroundColor)
                .border(
                    width = if (phase == SystemOverlayPhase.SUCCESS) 1.5.dp else if (d >= 0.70f) 0.9.dp else 1.1.dp,
                    brush = rimLightBrush,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(if (isExpanded) 12.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    val fadeDuration = ((140) / speedScale).toInt().coerceAtLeast(40)
                    fadeIn(animationSpec = tween(fadeDuration)) togetherWith fadeOut(animationSpec = tween(fadeDuration))
                },
                label = "SysIslandContent"
            ) { active ->
                if (active) {
                    SystemFaceIDSquareCanvas(
                        isSuccess = phase == SystemOverlayPhase.SUCCESS,
                        velocity = velocity
                    )
                } else {
                    SystemRestingPill(
                        darkness = d,
                        scale = s,
                        showGreenIndicator = showGreenIndicator
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemRestingPill(
    darkness: Float = 1.0f,
    scale: Float = 1.0f,
    showGreenIndicator: Boolean = false
) {
    val d = darkness.coerceIn(0f, 1f)
    val s = scale.coerceIn(0.5f, 1.4f)
    val glyphColor = if (d < 0.45f) Color(0xFFF1F5F9) else Color(0xFFB4BFCE)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = (10.dp * s).coerceAtLeast(4.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimalist Face ID wireframe glyph
        Canvas(modifier = Modifier.size((18.dp * s).coerceAtLeast(11.dp))) {
            val w = size.width
            val h = size.height

            val b = 3.5f * s
            val strokeW = (1.2f * s).coerceAtLeast(0.8f)
            drawLine(glyphColor, Offset(0f, 0f), Offset(b, 0f), strokeW)
            drawLine(glyphColor, Offset(0f, 0f), Offset(0f, b), strokeW)
            drawLine(glyphColor, Offset(w, 0f), Offset(w - b, 0f), strokeW)
            drawLine(glyphColor, Offset(w, 0f), Offset(w, b), strokeW)
            drawLine(glyphColor, Offset(0f, h), Offset(b, h), strokeW)
            drawLine(glyphColor, Offset(0f, h), Offset(0f, h - b), strokeW)
            drawLine(glyphColor, Offset(w, h), Offset(w - b, h), strokeW)
            drawLine(glyphColor, Offset(w, h), Offset(w, h - b), strokeW)

            // Eyes
            drawCircle(glyphColor, radius = 1.1f * s, center = Offset(w * 0.36f, h * 0.40f))
            drawCircle(glyphColor, radius = 1.1f * s, center = Offset(w * 0.64f, h * 0.40f))

            // Smile
            val smile = Path().apply {
                moveTo(w * 0.34f, h * 0.68f)
                quadraticTo(w * 0.5f, h * 0.82f, w * 0.66f, h * 0.68f)
            }
            drawPath(smile, glyphColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        }

        // Center micro accent: Clean stealth dark bar (no green light on center)
        Box(
            modifier = Modifier
                .width((22.dp * s).coerceAtLeast(10.dp))
                .height(2.dp)
                .clip(CircleShape)
                .background(if (d >= 0.70f) Color(0xFF141923) else Color(0xFF222B3A))
        )

        // Right-hand side: Green color privacy dot light + Camera cutout optics lens
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((5.dp * s).coerceAtLeast(3.dp))
        ) {
            if (showGreenIndicator) {
                // Luminous green privacy dot light on right-hand side
                Box(
                    modifier = Modifier
                        .size((7.5.dp * s).coerceAtLeast(4.5.dp))
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                        .border(0.8.dp, Color(0xFF69F0AE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((3.dp * s).coerceAtLeast(2.dp))
                            .clip(CircleShape)
                            .background(Color(0xFFE8FFF0))
                    )
                }
            }

            // Right side camera cutout lens optics
            Box(
                modifier = Modifier
                    .size((10.dp * s).coerceAtLeast(6.dp))
                    .clip(CircleShape)
                    .background(Color(0xFF080C13))
                    .border(0.8.dp, if (d < 0.45f) Color(0xFF334155) else Color(0xFF1A2230), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((4.dp * s).coerceAtLeast(2.5.dp))
                        .clip(CircleShape)
                        .background(Color(0xFF111E2E))
                )
            }
        }
    }
}

@Composable
private fun SystemFaceIDSquareCanvas(
    isSuccess: Boolean,
    velocity: Float = 1.0f
) {
    val speedScale = velocity.coerceIn(0.5f, 3.0f)
    val scanYAnim = remember { Animatable(0f) }
    val checkmarkAnim = remember { Animatable(0f) }

    LaunchedEffect(isSuccess, velocity) {
        if (!isSuccess) {
            checkmarkAnim.snapTo(0f)
            val laserDuration = ((650) / speedScale).toInt().coerceAtLeast(140)
            scanYAnim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(laserDuration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            scanYAnim.stop()
            checkmarkAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium * speedScale
                )
            )
        }
    }

    val faceAlpha by animateFloatAsState(
        targetValue = if (isSuccess) 0f else 1f,
        animationSpec = tween(((180) / speedScale).toInt().coerceAtLeast(50)),
        label = "SysFaceAlpha"
    )

    Canvas(modifier = Modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        val pad = 4f
        val boxColor = if (isSuccess) AuraSuccess else Color(0xFFE2E8F0)

        // 1. Classic Face ID Rounded Corner Brackets
        val cornerLen = 11f
        val strokeW = 2.4f

        // Top-Left corner
        drawLine(boxColor, Offset(pad, pad + cornerLen), Offset(pad, pad + 3f), strokeW, StrokeCap.Round)
        drawArc(boxColor, 180f, 90f, false, Offset(pad, pad), Size(6f, 6f), style = Stroke(strokeW, cap = StrokeCap.Round))
        drawLine(boxColor, Offset(pad + 3f, pad), Offset(pad + cornerLen, pad), strokeW, StrokeCap.Round)

        // Top-Right corner
        drawLine(boxColor, Offset(w - pad - cornerLen, pad), Offset(w - pad - 3f, pad), strokeW, StrokeCap.Round)
        drawArc(boxColor, 270f, 90f, false, Offset(w - pad - 6f, pad), Size(6f, 6f), style = Stroke(strokeW, cap = StrokeCap.Round))
        drawLine(boxColor, Offset(w - pad, pad + 3f), Offset(w - pad, pad + cornerLen), strokeW, StrokeCap.Round)

        // Bottom-Left corner
        drawLine(boxColor, Offset(pad, h - pad - cornerLen), Offset(pad, h - pad - 3f), strokeW, StrokeCap.Round)
        drawArc(boxColor, 90f, 90f, false, Offset(pad, h - pad - 6f), Size(6f, 6f), style = Stroke(strokeW, cap = StrokeCap.Round))
        drawLine(boxColor, Offset(pad + 3f, h - pad), Offset(pad + cornerLen, h - pad), strokeW, StrokeCap.Round)

        // Bottom-Right corner
        drawLine(boxColor, Offset(w - pad - cornerLen, h - pad), Offset(w - pad - 3f, h - pad), strokeW, StrokeCap.Round)
        drawArc(boxColor, 0f, 90f, false, Offset(w - pad - 6f, h - pad - 6f), Size(6f, 6f), style = Stroke(strokeW, cap = StrokeCap.Round))
        drawLine(boxColor, Offset(w - pad, h - pad - cornerLen), Offset(w - pad, h - pad - 3f), strokeW, StrokeCap.Round)

        // 2. Face Silhouette Inside (Fades out on SUCCESS)
        if (faceAlpha > 0.05f) {
            val faceColor = Color.White.copy(alpha = faceAlpha * 0.95f)
            drawCircle(faceColor, radius = 2.2f, center = Offset(w * 0.36f, h * 0.38f))
            drawCircle(faceColor, radius = 2.2f, center = Offset(w * 0.64f, h * 0.38f))
            drawLine(faceColor, Offset(w * 0.50f, h * 0.44f), Offset(w * 0.50f, h * 0.56f), 2f, StrokeCap.Round)

            val smilePath = Path().apply {
                moveTo(w * 0.34f, h * 0.68f)
                quadraticTo(w * 0.50f, h * 0.80f, w * 0.66f, h * 0.68f)
            }
            drawPath(smilePath, faceColor, style = Stroke(width = 2f, cap = StrokeCap.Round))

            // Sweeping laser scanline
            if (!isSuccess) {
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

        // 3. Iconic Animated Green Checkmark (Draws on SUCCESS)
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
