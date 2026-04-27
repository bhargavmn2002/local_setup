package com.signox.player.service

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager

class KioskModeManager(private val activity: Activity) {
    
    private val handler = Handler(Looper.getMainLooper())
    private var homeKeyWatcher: Runnable? = null
    private var isKioskModeActive = false
    
    companion object {
        private const val TAG = "KioskModeManager"
        private const val HOME_KEY_CHECK_INTERVAL = 15000L // Check every 15 seconds
    }
    
    fun enableKioskMode() {
        if (isKioskModeActive) return
        
        isKioskModeActive = true
        Log.d(TAG, "Enabling kiosk mode")
        
        // NOTE: setAsLauncher() removed - app no longer tries to become HOME launcher
        // This allows PIN exit to work properly
        
        // NOTE: Home key watcher disabled - app will NOT automatically return to foreground
        // Users can switch to other apps freely
        // startHomeKeyWatcher() - DISABLED
        
        // Additional kiosk mode settings
        enableImmersiveMode()
    }
    
    fun disableKioskMode() {
        if (!isKioskModeActive) return
        
        isKioskModeActive = false
        Log.d(TAG, "Disabling kiosk mode")
        
        // Stop monitoring
        stopHomeKeyWatcher()
    }
    
    private fun startHomeKeyWatcher() {
        // DISABLED: App will no longer automatically return to foreground
        // Users can freely switch to other apps
        /*
        homeKeyWatcher = object : Runnable {
            override fun run() {
                if (isKioskModeActive) {
                    checkAndReturnToApp()
                    handler.postDelayed(this, HOME_KEY_CHECK_INTERVAL)
                }
            }
        }
        handler.post(homeKeyWatcher!!)
        */
    }
    
    private fun stopHomeKeyWatcher() {
        homeKeyWatcher?.let { handler.removeCallbacks(it) }
        homeKeyWatcher = null
    }
    
    private fun checkAndReturnToApp() {
        // DISABLED: No longer checks if app is in foreground
        /*
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        
        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            if (topActivity?.packageName != activity.packageName) {
                // Another app is in foreground, bring our app back
                bringAppToFront()
            }
        }
        */
    }
    
    private fun bringAppToFront() {
        // DISABLED: No longer brings app to foreground automatically
        /*
        try {
            val intent = Intent(activity, activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to front", e)
        }
        */
    }
    
    private fun enableImmersiveMode() {
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Keep screen on
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Prevent status bar pull-down
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && isKioskModeActive) {
            enableImmersiveMode()
        }
    }
    
    fun isKioskModeEnabled(): Boolean {
        return isKioskModeActive
    }
}
