package com.signox.offlinemanager.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.signox.offlinemanager.R
import com.signox.offlinemanager.databinding.ActivityMainBinding
import com.signox.offlinemanager.utils.AppMonitor

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AppMonitor.logActivityLifecycle("MainActivity", "onCreate - Start")
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            AppMonitor.logActivityLifecycle("MainActivity", "Layout inflated successfully")
            
            setupNavigation()
            
            AppMonitor.logActivityLifecycle("MainActivity", "onCreate - Complete")
        } catch (e: Exception) {
            AppMonitor.logError("MainActivity.onCreate", e)
            throw e
        }
    }
    
    private fun setupNavigation() {
        try {
            AppMonitor.logActivityLifecycle("MainActivity", "Setting up navigation")
            
            setSupportActionBar(binding.toolbar)
            AppMonitor.logActivityLifecycle("MainActivity", "Toolbar set successfully")
            
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            
            appBarConfiguration = AppBarConfiguration(
                setOf(R.id.dashboardFragment)
            )
            
            setupActionBarWithNavController(navController, appBarConfiguration)
            
            AppMonitor.logActivityLifecycle("MainActivity", "Navigation setup complete")
        } catch (e: Exception) {
            AppMonitor.logError("MainActivity.setupNavigation", e)
            throw e
        }
    }
    
    override fun onStart() {
        super.onStart()
        AppMonitor.logActivityLifecycle("MainActivity", "onStart")
    }
    
    override fun onResume() {
        super.onResume()
        AppMonitor.logActivityLifecycle("MainActivity", "onResume")
    }
    
    override fun onPause() {
        super.onPause()
        AppMonitor.logActivityLifecycle("MainActivity", "onPause")
    }
    
    override fun onStop() {
        super.onStop()
        AppMonitor.logActivityLifecycle("MainActivity", "onStop")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AppMonitor.logActivityLifecycle("MainActivity", "onDestroy")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}