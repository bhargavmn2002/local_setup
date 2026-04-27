package com.signox.dashboard.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.FragmentUserDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class UserDetailsFragment : Fragment() {
    
    private var _binding: FragmentUserDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedUser.collect { user ->
                user?.let {
                    // Basic Information
                    binding.tvEmail.text = it.email
                    binding.tvRole.text = it.role.name.replace("_", " ")
                    binding.tvStatus.text = if (it.isActive) "Active" else "Inactive"
                    binding.chipStatus.text = if (it.isActive) "Active" else "Inactive"
                    binding.chipStatus.setChipBackgroundColorResource(
                        if (it.isActive) android.R.color.holo_green_light 
                        else android.R.color.holo_red_light
                    )
                    
                    // Created Date
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                        val date = inputFormat.parse(it.createdAt)
                        binding.tvCreatedAt.text = date?.let { d -> outputFormat.format(d) } ?: it.createdAt
                    } catch (e: Exception) {
                        binding.tvCreatedAt.text = it.createdAt
                    }
                    
                    // Role-specific information
                    when (it.role.name) {
                        "CLIENT_ADMIN" -> {
                            binding.layoutClientProfile.visibility = View.VISIBLE
                            binding.layoutStaffRole.visibility = View.GONE
                            
                            it.clientProfile?.let { profile ->
                                binding.tvCompanyName.text = profile.companyName ?: "N/A"
                                binding.tvMaxDisplays.text = profile.maxDisplays?.toString() ?: "N/A"
                                binding.tvMaxUsers.text = profile.maxUsers?.toString() ?: "N/A"
                                binding.tvMaxStorage.text = "${profile.maxStorageMB ?: 0} MB"
                                binding.tvContactEmail.text = profile.contactEmail ?: "N/A"
                                binding.tvContactPhone.text = profile.contactPhone ?: "N/A"
                                
                                // License Expiry
                                profile.licenseExpiry?.let { expiry ->
                                    try {
                                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        val date = inputFormat.parse(expiry)
                                        binding.tvLicenseExpiry.text = date?.let { d -> outputFormat.format(d) } ?: expiry
                                    } catch (e: Exception) {
                                        binding.tvLicenseExpiry.text = expiry
                                    }
                                } ?: run {
                                    binding.tvLicenseExpiry.text = "No expiry"
                                }
                            }
                        }
                        "STAFF" -> {
                            binding.layoutClientProfile.visibility = View.GONE
                            binding.layoutStaffRole.visibility = View.VISIBLE
                            binding.tvStaffRole.text = it.staffRole?.name?.replace("_", " ") ?: "N/A"
                        }
                        else -> {
                            binding.layoutClientProfile.visibility = View.GONE
                            binding.layoutStaffRole.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
