package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.DynamicIslandOverlayService
import com.example.service.OverlayTrigger
import com.example.ui.components.FullScreenPreviewDialog
import com.example.ui.components.LockScreenOverlay
import com.example.ui.screens.AnimationsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTheme
import com.example.ui.theme.OrbitronFontFamily
import com.example.viewmodel.AuraUnlockViewModel

enum class AuraTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    ANIMATIONS("Animations", Icons.Default.AutoAwesome),
    STUDIO("Studio", Icons.Default.Tune),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

@Composable
fun AuraApp(
    viewModel: AuraUnlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val showFullScreenPreview by viewModel.showFullScreenPreview.collectAsState()
    val isDynamicIslandEnabled by viewModel.isDynamicIslandEnabled.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val studioConfig by viewModel.studioConfig.collectAsState()

    // Automatically ensure system overlay service is active at the top camera center
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Settings.canDrawOverlays(context)) {
                    DynamicIslandOverlayService.startService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Automatically execute Face ID unlock sequence on app open and animate top-center overlay
    LaunchedEffect(Unit) {
        if (Settings.canDrawOverlays(context)) {
            DynamicIslandOverlayService.startService(context)
        }
        viewModel.triggerAppOpenUnlock()
        OverlayTrigger.fire(OverlayTrigger.Source.APP_OPENED)
    }

    // Handle Android system back gesture to navigate back to Home if in another tab
    if (selectedTabIndex != 0) {
        BackHandler {
            selectedTabIndex = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AuraBackground)) {
        // Main Application Scaffolding
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AuraBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    containerColor = AuraSurface,
                    contentColor = AuraTextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    AuraTab.values().forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title.uppercase(),
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    letterSpacing = 0.5.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF003733),
                                selectedTextColor = studioConfig.accentColor,
                                indicatorColor = studioConfig.accentColor,
                                unselectedIconColor = AuraTextMuted,
                                unselectedTextColor = AuraTextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = selectedTabIndex,
                    label = "TabTransition"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAnimations = { selectedTabIndex = 1 },
                            onNavigateToStudio = { selectedTabIndex = 2 }
                        )
                        1 -> AnimationsScreen(
                            viewModel = viewModel,
                            onNavigateToStudio = { selectedTabIndex = 2 }
                        )
                        2 -> StudioScreen(
                            viewModel = viewModel
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Lock Screen Curtain Overlay (shown until Face ID finishes authenticating on app start / lock)
        AnimatedVisibility(
            visible = !isAppUnlocked,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            LockScreenOverlay(
                scanState = scanState,
                onTapScreen = {
                    viewModel.triggerAppOpenUnlock()
                    OverlayTrigger.fire(OverlayTrigger.Source.APP_OPENED)
                }
            )
        }

        // Full Screen Scan Simulation Overlay
        if (showFullScreenPreview) {
            FullScreenPreviewDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeFullScreenPreview() }
            )
        }
    }
}
