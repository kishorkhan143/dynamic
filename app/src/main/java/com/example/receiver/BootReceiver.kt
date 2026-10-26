package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.service.DynamicIslandOverlayService

/**
 * Ensures the Dynamic Island overlay service automatically restarts after device reboot or package upgrade
 * so the dynamic island remains persistently active across all system sessions.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            if (Settings.canDrawOverlays(context)) {
                DynamicIslandOverlayService.startService(context)
            }
        }
    }
}
