package com.signox.offlinemanager.utils

import android.app.Application
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Application monitoring utility using OkHttp for network monitoring
 * and comprehensive logging for debugging app crashes and performance
 */
object AppMonitor {
    
    private const val TAG = "SignoXMonitor"
    
    private val httpClient: OkHttpClient by lazy {
        createHttpClient()
    }
    
    /**
     * Initialize monitoring system
     */
    fun initialize(application: Application) {
        Log.i(TAG, "=== SignoX Offline Manager Monitor Initialized ===")
        Log.i(TAG, "App Version: ${getAppVersion(application)}")
        Log.i(TAG, "Device Info: ${getDeviceInfo()}")
        Log.i(TAG, "Memory Info: ${getMemoryInfo()}")
        
        // Set up uncaught exception handler
        setupExceptionHandler()
        
        // Log app lifecycle
        logAppLifecycle("App Started")
    }
    
    /**
     * Create OkHttp client with comprehensive logging
     */
    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("$TAG-HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Get HTTP client for network operations
     */
    fun getOkHttpClient(): OkHttpClient = httpClient
    
    /**
     * Log app lifecycle events
     */
    fun logAppLifecycle(event: String) {
        Log.i(TAG, "=== LIFECYCLE: $event ===")
        Log.i(TAG, "Timestamp: ${System.currentTimeMillis()}")
        Log.i(TAG, "Memory Usage: ${getMemoryUsage()}")
    }
    
    /**
     * Log activity lifecycle
     */
    fun logActivityLifecycle(activityName: String, event: String) {
        Log.i(TAG, "ACTIVITY: $activityName - $event")
    }
    
    /**
     * Log fragment lifecycle
     */
    fun logFragmentLifecycle(fragmentName: String, event: String) {
        Log.i(TAG, "FRAGMENT: $fragmentName - $event")
    }
    
    /**
     * Log database operations
     */
    fun logDatabaseOperation(operation: String, table: String, details: String = "") {
        Log.d("$TAG-DB", "DB Operation: $operation on $table - $details")
    }
    
    /**
     * Log media operations
     */
    fun logMediaOperation(operation: String, mediaType: String, details: String = "") {
        Log.d("$TAG-MEDIA", "Media Operation: $operation - $mediaType - $details")
    }
    
    /**
     * Log errors with context
     */
    fun logError(context: String, error: Throwable) {
        Log.e(TAG, "ERROR in $context: ${error.message}", error)
        Log.e(TAG, "Stack trace: ${error.stackTraceToString()}")
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformance(operation: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        Log.d("$TAG-PERF", "Performance: $operation took ${duration}ms")
    }
    
    /**
     * Setup uncaught exception handler
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "=== UNCAUGHT EXCEPTION ===")
            Log.e(TAG, "Thread: ${thread.name}")
            Log.e(TAG, "Exception: ${exception.message}")
            Log.e(TAG, "Stack trace: ${exception.stackTraceToString()}")
            Log.e(TAG, "Memory at crash: ${getMemoryUsage()}")
            Log.e(TAG, "=== END CRASH LOG ===")
            
            // Call the default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    /**
     * Get app version info
     */
    private fun getAppVersion(application: Application): String {
        return try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get device information
     */
    private fun getDeviceInfo(): String {
        return "Model: ${android.os.Build.MODEL}, " +
                "Android: ${android.os.Build.VERSION.RELEASE}, " +
                "API: ${android.os.Build.VERSION.SDK_INT}"
    }
    
    /**
     * Get memory information
     */
    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        return "Max: ${maxMemory}MB, Total: ${totalMemory}MB, Free: ${freeMemory}MB"
    }
    
    /**
     * Get current memory usage
     */
    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        return "${usedMemory}MB / ${maxMemory}MB"
    }
}