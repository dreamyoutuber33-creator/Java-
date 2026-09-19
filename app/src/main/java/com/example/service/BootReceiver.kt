package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver to automatically re-launch Java Background Service
 * on device boot or app update if the user had enabled it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("java_assistant_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("pref_background_service", false)
            if (isEnabled) {
                JavaBackgroundService.start(context)
            }
        }
    }
}
