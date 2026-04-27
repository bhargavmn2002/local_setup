package com.signox.dashboard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.databinding.FragmentClientAdminDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClientAdminDashboardFragment : Fragment() {
    
    private var _binding: FragmentClientAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientAdminDashboardBinding.inflate(inflater, container, false)
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
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { profile ->
                        // Display client admin's own name or email
                        val userName = profile.user.name?.takeIf { it.isNotBlank() } ?: profile.user.email
                        binding.tvEmail.text = userName
                        
                        binding.tvRole.text = "Client Administrator"
                        binding.tvCompany.text = profile.hierarchy?.companyName ?: "—"
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
        
        viewModel.summaryState.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { summary ->
                        binding.tvUserAdmins.text = summary.userAdmins?.toString() ?: "—"
                        binding.tvDisplays.text = "${summary.totalDisplays ?: 0}/${summary.displayLimit ?: 0}"
                        binding.tvLicenseStatus.text = summary.license?.status ?: "—"
                        
                        summary.license?.expiry?.let { expiry ->
                            binding.tvLicenseExpiry.text = "Expires: $expiry"
                            binding.tvLicenseExpiry.visibility = View.VISIBLE
                        } ?: run {
                            binding.tvLicenseExpiry.visibility = View.GONE
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
