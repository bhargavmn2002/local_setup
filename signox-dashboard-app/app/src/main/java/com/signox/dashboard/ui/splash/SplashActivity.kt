package com.signox.dashboard.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.data.local.ServerConfigManager
import com.signox.dashboard.data.local.TokenManager
import com.signox.dashboard.databinding.ActivitySplashBinding
import com.signox.dashboard.ui.auth.LoginActivity
import com.signox.dashboard.ui.config.ServerConfigActivity
import com.signox.dashboard.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    @Inject
    lateinit var serverConfigManager: ServerConfigManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkAppStatus()
    }
    
    private fun checkAppStatus() {
        lifecycleScope.launch {
            // Show splash for at least 1.5 seconds
            delay(1500)
            
            // Check if server is configured, if not use default
            val isServerConfigured = serverConfigManager.isServerConfigured()
            
            if (!isServerConfigured) {
                // Save default server URL
                serverConfigManager.saveServerUrl(ServerConfigManager.DEFAULT_SERVER_URL)
            }
            
            // Check auth status
            val token = tokenManager.getToken().first()
            
            if (!token.isNullOrEmpty()) {
                // User is logged in, go to main dashboard
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // User not logged in, go to login
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            
            finish()
        }
    }
}
