package com.signox.dashboard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.R
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.local.ServerConfigManager
import com.signox.dashboard.data.model.UserRole
import com.signox.dashboard.databinding.ActivityLoginBinding
import com.signox.dashboard.ui.main.MainActivity
import com.signox.dashboard.utils.AnimationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    @Inject
    lateinit var serverConfigManager: ServerConfigManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        animateViews()
    }
    
    private fun animateViews() {
        // Animate login card with bounce effect
        AnimationUtils.bounceIn(binding.cardLogin, 100)
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            // Add pulse animation on button click
            AnimationUtils.pulse(binding.btnLogin)
            
            viewModel.login(email, password)
        }
    }
    
    private fun observeViewModel() {
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoading(true)
                }
                is NetworkResult.Success -> {
                    showLoading(false)
                    // Navigation handled by navigationEvent
                }
                is NetworkResult.Error -> {
                    showLoading(false)
                    showError(result.message ?: "Login failed")
                }
            }
        }
        
        viewModel.navigationEvent.observe(this) { roleData ->
            roleData?.let {
                navigateToDashboard(it.first, it.second)
                viewModel.resetNavigationEvent()
            }
        }
    }
    
    private fun navigateToDashboard(role: UserRole, staffRole: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_USER_ROLE, role.name)
            staffRole?.let { putExtra(MainActivity.EXTRA_STAFF_ROLE, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        
        // Shake animation for error
        AnimationUtils.shake(binding.tvError)
        AnimationUtils.shake(binding.cardLogin)
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
