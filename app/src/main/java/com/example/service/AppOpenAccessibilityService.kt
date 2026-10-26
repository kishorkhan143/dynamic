package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent

/**
 * Lightweight Accessibility Service that detects when applications are launched.
 *
 * Guaranteed Behavior:
 * - TRIGGERS: Strictly when launching/opening a user application (from Home, Recents, or App Switcher).
 * - NEVER TRIGGERS: When closing an app, pressing Back until exit, minimizing, returning to Home,
 *   or opening the Recent Apps overview.
 */
class AppOpenAccessibilityService : AccessibilityService() {

    private var lastPackageName: String? = null
    private var lastTriggerTimeMs: Long = 0L
    private var wasAtHomeOrRecents: Boolean = true

    private val cachedLauncherPackages = mutableSetOf<String>()
    private var lastLauncherCheckTimeMs: Long = 0L

    companion object {
        private const val DEBOUNCE_INTERVAL_MS = 600L

        private val KNOWN_LAUNCHERS = setOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.launcher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.oneplus.launcher",
            "com.transsion.hilauncher",
            "com.motorola.launcher3",
            "com.teslacoilsw.launcher",
            "ch.deletescape.lawnchair",
            "com.actionlauncher.playstore",
            "com.microsoft.launcher"
        )

        fun isEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, AppOpenAccessibilityService::class.java).flattenToString()
            val shortComponentName = ComponentName(context, AppOpenAccessibilityService::class.java).flattenToShortString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val component = colonSplitter.next()
                if (component.equals(expectedComponentName, ignoreCase = true) ||
                    component.equals(shortComponentName, ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkgName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // 1. Ignore transient system overlays, dialogs, toasts, volume bars, and keyboards
        if (className.contains("Dialog", ignoreCase = true) ||
            className.contains("Toast", ignoreCase = true) ||
            className.contains("PopupWindow", ignoreCase = true) ||
            className.contains("InputMethod", ignoreCase = true) ||
            className.contains("VolumeDialog", ignoreCase = true) ||
            className.contains("Dropdown", ignoreCase = true)
        ) {
            return
        }

        // 2. HOME / LAUNCHER / RECENTS / SYSTEM NAVIGATION:
        // When user navigates to Home, opens Recent Apps (Overview), or minimizes/closes an app:
        // We MUST NEVER trigger the Face ID dynamic island animation!
        // Record that user is now at Home or Recents so that subsequent app opens will trigger.
        if (isHomeOrLauncher(pkgName) || isRecentAppsOrSystemUi(pkgName, className)) {
            wasAtHomeOrRecents = true
            lastPackageName = null
            return
        }

        // 3. System packages (Android framework, permissions, installer, keyboard IME):
        if (isSystemPackage(pkgName)) {
            return
        }

        // 4. Our own app: handled within MainActivity lifecycle
        if (pkgName == packageName) {
            wasAtHomeOrRecents = false
            lastPackageName = pkgName
            return
        }

        val now = SystemClock.elapsedRealtime()

        // 5. Ignore repeat events while navigating within the same open application
        if (pkgName == lastPackageName && !wasAtHomeOrRecents) {
            return
        }

        // 6. Only trigger for genuine launchable user applications
        if (!isLaunchableUserApp(pkgName)) {
            return
        }

        // 7. Debounce between triggers to avoid multiple events on a single app launch
        if ((now - lastTriggerTimeMs) < DEBOUNCE_INTERVAL_MS) {
            return
        }

        // Confirmed App Launch!
        lastPackageName = pkgName
        lastTriggerTimeMs = now
        wasAtHomeOrRecents = false

        // Trigger the Face ID dynamic island animation at the top-center camera notch
        triggerAppOpenAnimation()
    }

    override fun onInterrupt() {
        // No-op
    }

    private fun triggerAppOpenAnimation() {
        if (Settings.canDrawOverlays(this)) {
            if (!DynamicIslandOverlayService.isRunning) {
                DynamicIslandOverlayService.startService(this, triggerAppOpen = true)
            } else {
                OverlayTrigger.fire(OverlayTrigger.Source.APP_OPENED)
            }
        }
    }

    private fun isRecentAppsOrSystemUi(pkgName: String, className: String): Boolean {
        if (pkgName == "com.android.systemui") {
            return true
        }
        if (className.contains("Recents", ignoreCase = true) ||
            className.contains("Overview", ignoreCase = true) ||
            className.contains("TaskSwitcher", ignoreCase = true) ||
            className.contains("Quickstep", ignoreCase = true) ||
            className.contains("RecentTasks", ignoreCase = true) ||
            className.contains("Taskbar", ignoreCase = true)
        ) {
            return true
        }
        return false
    }

    private fun isSystemPackage(pkgName: String): Boolean {
        return pkgName == "com.android.systemui" ||
                pkgName == "android" ||
                pkgName == "com.google.android.setupwizard" ||
                pkgName == "com.android.setupwizard" ||
                pkgName == "com.google.android.packageinstaller" ||
                pkgName == "com.android.packageinstaller" ||
                pkgName == "com.google.android.permissioncontroller" ||
                pkgName == "com.android.permissioncontroller" ||
                pkgName == getDefaultInputMethodPackage() ||
                pkgName.contains("inputmethod", ignoreCase = true)
    }

    private fun isLaunchableUserApp(pkgName: String): Boolean {
        if (pkgName == packageName || isSystemPackage(pkgName) || isHomeOrLauncher(pkgName)) {
            return false
        }

        // Primary: check launch intent via PackageManager
        try {
            if (packageManager.getLaunchIntentForPackage(pkgName) != null) {
                return true
            }
        } catch (_: Exception) {}

        // Fallback: check queryIntentActivities for Intent.ACTION_MAIN + CATEGORY_LAUNCHER
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(pkgName)
            }
            val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
            }
            matches.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun isHomeOrLauncher(pkgName: String): Boolean {
        if (KNOWN_LAUNCHERS.contains(pkgName) ||
            pkgName.contains("launcher", ignoreCase = true) ||
            pkgName.contains("quickstep", ignoreCase = true) ||
            pkgName.contains("home", ignoreCase = true) ||
            pkgName.contains("trebuchet", ignoreCase = true)
        ) {
            return true
        }

        // Check dynamically resolved home launcher packages with periodic refresh
        val now = SystemClock.elapsedRealtime()
        if (cachedLauncherPackages.isEmpty() || (now - lastLauncherCheckTimeMs) > 60000L) {
            lastLauncherCheckTimeMs = now
            try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(intent, 0)
                }
                for (info in resolveList) {
                    info.activityInfo?.packageName?.let { cachedLauncherPackages.add(it) }
                }
            } catch (_: Exception) {}
        }

        return cachedLauncherPackages.contains(pkgName)
    }

    private fun getDefaultInputMethodPackage(): String? {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return defaultIme?.substringBefore('/')
    }
}
