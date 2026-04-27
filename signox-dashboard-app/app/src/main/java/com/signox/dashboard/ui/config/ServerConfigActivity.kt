package com.signox.dashboard.ui.config

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.ActivityServerConfigBinding
import com.signox.dashboard.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ServerConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityServerConfigBinding
    private val viewModel: ServerConfigViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Load existing server URL if available
        lifecycleScope.launch {
            viewModel.loadServerUrl()
        }
        
        binding.btnConnect.setOnClickListener {
            var serverUrl = binding.etServerUrl.text.toString().trim()
            
            if (serverUrl.isEmpty()) {
                binding.tilServerUrl.error = "Please enter server URL"
                return@setOnClickListener
            }
            
            // Auto-add http:// if not present
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                serverUrl = "http://$serverUrl"
                binding.etServerUrl.setText(serverUrl)
            }
            
            if (!isValidUrl(serverUrl)) {
                binding.tilServerUrl.error = "Please enter a valid URL (e.g., http://192.168.1.100:5000)"
                return@setOnClickListener
            }
            
            binding.tilServerUrl.error = null
            
            // Test connection and save if successful
            viewModel.testAndSaveServerUrl(serverUrl)
        }
        
        binding.tvExampleUrl.setOnClickListener {
            binding.etServerUrl.setText("192.168.1.231:5000")
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.serverUrl.collect { url ->
                if (!url.isNullOrEmpty()) {
                    binding.etServerUrl.setText(url)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.btnConnect.isEnabled = false
                    binding.btnConnect.text = "Connecting..."
                    binding.progressBar.visibility = View.VISIBLE
                    binding.etServerUrl.isEnabled = false
                } else {
                    binding.btnConnect.isEnabled = true
                    binding.btnConnect.text = "Connect"
                    binding.progressBar.visibility = View.GONE
                    binding.etServerUrl.isEnabled = true
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.saveSuccess.collect { success ->
                if (success) {
                    Toast.makeText(
                        this@ServerConfigActivity,
                        "Connected successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to login
                    startActivity(Intent(this@ServerConfigActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collect { errorMessage ->
                if (errorMessage.isNotEmpty()) {
                    binding.tilServerUrl.error = errorMessage
                    Toast.makeText(
                        this@ServerConfigActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
