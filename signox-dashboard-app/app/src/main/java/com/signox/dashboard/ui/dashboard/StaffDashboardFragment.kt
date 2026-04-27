package com.signox.dashboard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.databinding.FragmentStaffDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StaffDashboardFragment : Fragment() {
    
    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        viewModel.loadDashboardData()
    }
    
    private fun setupUI() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
    
    private fun observeViewModel() {
        viewModel.profileState.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { profile ->
                        // Display staff member's own name or email
                        val userName = profile.user.name?.takeIf { it.isNotBlank() } ?: profile.user.email
                        binding.tvEmail.text = userName
                        
                        val staffRoleName = when (profile.user.staffRole?.name) {
                            "DISPLAY_MANAGER" -> "Display Manager"
                            "BROADCAST_MANAGER" -> "Broadcast Manager"
                            "CONTENT_MANAGER" -> "Content Manager"
                            "CMS_VIEWER" -> "CMS Viewer"
                            "POP_MANAGER" -> "Proof of Play Manager"
                            else -> "Staff Member"
                        }
                        binding.tvRole.text = staffRoleName
                        
                        // Display company name
                        binding.tvCompany.text = profile.hierarchy?.companyName ?: "—"
                        
                        // Display client admin info
                        profile.hierarchy?.clientAdmin?.let { admin ->
                            val adminName = admin.name?.takeIf { it.isNotBlank() } ?: admin.email
                            binding.tvClientAdmin.text = adminName
                        } ?: run {
                            binding.tvClientAdmin.text = "—"
                        }
                        
                        // Display user admin info
                        profile.hierarchy?.userAdmin?.let { admin ->
                            val adminName = admin.name?.takeIf { it.isNotBlank() } ?: admin.email
                            binding.tvUserAdmin.text = adminName
                        } ?: run {
                            binding.tvUserAdmin.text = "—"
                        }
                    }
                }
                is NetworkResult.Error -> {
                    // Handle error
                }
                is NetworkResult.Loading -> {
                    // Show loading
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
