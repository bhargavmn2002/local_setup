package com.signox.player.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.signox.player.MainActivity

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot completed, starting SignoX Player")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                startApp(context)
            }
        }
    }
    
    private fun startApp(context: Context) {
        try {
            // Check if user intentionally exited (optional - remove if you want boot to always start)
            val prefs = context.getSharedPreferences("watchdog_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("user_exited", false)) {
                Log.d(TAG, "User exited app - not auto-starting on boot")
                return
            }
            
            // Start the watchdog service first (handle Android 8.0+ restrictions)
            val watchdogIntent = Intent(context, com.signox.player.service.WatchdogService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(watchdogIntent)
                    Log.d(TAG, "Started WatchdogService as foreground service (Android 8.0+)")
                } else {
                    context.startService(watchdogIntent)
                    Log.d(TAG, "Started WatchdogService as background service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start WatchdogService", e)
            }
            
            // Then start the main activity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("auto_started", true)
            }
            context.startActivity(intent)
            Log.d(TAG, "SignoX Player started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SignoX Player", e)
        }
    }
}