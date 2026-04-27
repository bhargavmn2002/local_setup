package com.signox.offlinemanager

import android.app.Application
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.utils.AppMonitor

class SignoXOfflineApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize monitoring system
        AppMonitor.initialize(this)
        AppMonitor.logAppLifecycle("Application Created")
    }
    
    override fun onTerminate() {
        AppMonitor.logAppLifecycle("Application Terminated")
        super.onTerminate()
    }
    
    override fun onLowMemory() {
        AppMonitor.logAppLifecycle("Low Memory Warning")
        super.onLowMemory()
    }
}