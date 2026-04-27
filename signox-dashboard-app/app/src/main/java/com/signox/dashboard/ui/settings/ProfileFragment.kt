package com.signox.dashboard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
        setupListeners()
        
        // Load profile data
        viewModel.loadProfile()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profile.collect { profile ->
                profile?.let {
                    // Profile Information
                    binding.tvEmail.text = it.email
                    
                    // Format role display
                    val roleDisplay = when (it.role) {
                        "CLIENT_ADMIN" -> "Client Administrator"
                        "USER_ADMIN" -> "User Administrator"
                        "STAFF" -> {
                            val staffRoleDisplay = when (it.staffRole) {
                                "CONTENT_MANAGER" -> "Content Manager"
                                "DISPLAY_MANAGER" -> "Display Manager"
                                "POP_MANAGER" -> "Proof of Play Manager"
                                "CMS_VIEWER" -> "Viewer"
                                else -> it.staffRole ?: "Staff"
                            }
                            "Staff Member - $staffRoleDisplay"
                        }
                        else -> it.role.replace("_", " ")
                    }
                    binding.tvRole.text = roleDisplay
                    
                    // Account Status
                    binding.tvAccountStatus.text = "Active"
                    
                    // Member Since
                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    binding.tvMemberSince.text = dateFormat.format(it.createdAt)
                    
                    // Organization Hierarchy
                    binding.tvCompany.text = it.companyName ?: "N/A"
                    
                    // Show hierarchy based on role
                    when (it.role) {
                        "CLIENT_ADMIN" -> {
                            // CLIENT_ADMIN doesn't see client admin or user admin
                            binding.labelClientAdmin.visibility = View.GONE
                            binding.tvClientAdmin.visibility = View.GONE
                            binding.labelUserAdmin.visibility = View.GONE
                            binding.tvUserAdmin.visibility = View.GONE
                        }
                        "USER_ADMIN" -> {
                            // USER_ADMIN sees client admin only
                            // Need to fetch from hierarchy endpoint
                            viewModel.loadHierarchy()
                        }
                        "STAFF" -> {
                            // STAFF sees both client admin and user admin
                            viewModel.loadHierarchy()
                        }
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hierarchy.collect { hierarchy ->
                hierarchy?.let {
                    // Show Client Admin if available
                    if (it.clientAdmin != null) {
                        binding.labelClientAdmin.visibility = View.VISIBLE
                        binding.tvClientAdmin.visibility = View.VISIBLE
                        binding.tvClientAdmin.text = it.clientAdmin.name ?: it.clientAdmin.email
                    }
                    
                    // Show User Admin if available
                    if (it.userAdmin != null) {
                        binding.labelUserAdmin.visibility = View.VISIBLE
                        binding.tvUserAdmin.visibility = View.VISIBLE
                        binding.tvUserAdmin.text = it.userAdmin.name ?: it.userAdmin.email
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnUpdatePassword.isEnabled = !isLoading
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
                    
                    // Clear password fields on success
                    binding.etCurrentPassword.text?.clear()
                    binding.etNewPassword.text?.clear()
                    binding.etConfirmPassword.text?.clear()
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }
    
    private fun updatePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Validation
        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.error = "Current password is required"
            return
        }
        
        if (newPassword.isEmpty()) {
            binding.etNewPassword.error = "New password is required"
            return
        }
        
        if (newPassword.length < 6) {
            binding.etNewPassword.error = "Password must be at least 6 characters"
            return
        }
        
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            return
        }
        
        if (newPassword != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return
        }
        
        // Call viewModel to update password
        viewModel.changePassword(currentPassword, newPassword)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
