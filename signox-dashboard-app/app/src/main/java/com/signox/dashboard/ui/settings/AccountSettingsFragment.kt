package com.signox.dashboard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.FragmentAccountSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountSettingsFragment : Fragment() {
    
    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
        
        // Load account info
        viewModel.loadAccountInfo()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountInfo.collect { accountInfo ->
                accountInfo?.let {
                    updateUI(it)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
    }
    
    private fun updateUI(accountInfo: com.signox.dashboard.data.model.AccountInfo) {
        // Company name
        binding.tvCompanyName.text = accountInfo.companyName ?: "N/A"
        
        // Subscription status
        binding.chipSubscriptionStatus.text = accountInfo.subscriptionStatus
        
        // License expiry
        binding.tvLicenseExpiry.text = accountInfo.licenseExpiry ?: "N/A"
        
        // Displays usage
        val maxDisplays = accountInfo.maxDisplays ?: 0
        val currentDisplays = accountInfo.currentDisplays
        binding.tvDisplaysUsage.text = "$currentDisplays / $maxDisplays"
        if (maxDisplays > 0) {
            val displayProgress = (currentDisplays.toFloat() / maxDisplays * 100).toInt()
            binding.progressDisplays.progress = displayProgress
        }
        
        // Users usage
        val maxUsers = accountInfo.maxUsers ?: 0
        val currentUsers = accountInfo.currentUsers
        binding.tvUsersUsage.text = "$currentUsers / $maxUsers"
        if (maxUsers > 0) {
            val userProgress = (currentUsers.toFloat() / maxUsers * 100).toInt()
            binding.progressUsers.progress = userProgress
        }
        
        // Storage usage
        val maxStorage = accountInfo.maxStorageMB ?: 0
        val currentStorage = accountInfo.currentStorageMB
        binding.tvStorageUsage.text = "$currentStorage MB / $maxStorage MB"
        if (maxStorage > 0) {
            val storageProgress = (currentStorage.toFloat() / maxStorage * 100).toInt()
            binding.progressStorage.progress = storageProgress
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
