package com.signox.dashboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SignoXApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: SignoXApplication
            private set
    }
}
