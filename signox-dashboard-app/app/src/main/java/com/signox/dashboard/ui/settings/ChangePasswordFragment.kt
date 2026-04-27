package com.signox.dashboard.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.R
import com.signox.dashboard.databinding.FragmentChangePasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {
    
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
        setupListeners()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnChangePassword.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                    // Navigate back on success
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
        
        // Password strength indicator
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }
        })
    }
    
    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.layoutPasswordStrength.visibility = View.GONE
            return
        }
        
        binding.layoutPasswordStrength.visibility = View.VISIBLE
        
        val strength = calculatePasswordStrength(password)
        binding.tvPasswordStrength.text = when (strength) {
            0, 1 -> "Weak"
            2 -> "Medium"
            3, 4 -> "Strong"
            else -> "Very Strong"
        }
        
        val color = when (strength) {
            0, 1 -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            2 -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        }
        binding.tvPasswordStrength.setTextColor(color)
    }
    
    private fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        
        if (password.length >= 8) strength++
        if (password.any { it.isUpperCase() }) strength++
        if (password.any { it.isLowerCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++
        
        return strength
    }
    
    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        // Validation
        if (currentPassword.isEmpty()) {
            binding.layoutCurrentPassword.error = "Current password is required"
            return
        }
        
        if (newPassword.isEmpty()) {
            binding.layoutNewPassword.error = "New password is required"
            return
        }
        
        if (newPassword.length < 8) {
            binding.layoutNewPassword.error = "Password must be at least 8 characters"
            return
        }
        
        if (!newPassword.any { it.isUpperCase() }) {
            binding.layoutNewPassword.error = "Password must contain at least one uppercase letter"
            return
        }
        
        if (!newPassword.any { it.isLowerCase() }) {
            binding.layoutNewPassword.error = "Password must contain at least one lowercase letter"
            return
        }
        
        if (!newPassword.any { it.isDigit() }) {
            binding.layoutNewPassword.error = "Password must contain at least one number"
            return
        }
        
        if (newPassword != confirmPassword) {
            binding.layoutConfirmPassword.error = "Passwords do not match"
            return
        }
        
        if (currentPassword == newPassword) {
            binding.layoutNewPassword.error = "New password must be different from current password"
            return
        }
        
        // Clear errors
        binding.layoutCurrentPassword.error = null
        binding.layoutNewPassword.error = null
        binding.layoutConfirmPassword.error = null
        
        // Call API
        viewModel.changePassword(currentPassword, newPassword)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
