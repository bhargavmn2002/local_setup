package com.signox.dashboard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.databinding.FragmentUserAdminDashboardBinding
import com.signox.dashboard.utils.AnimationUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserAdminDashboardFragment : Fragment() {
    
    private var _binding: FragmentUserAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        animateViews()
        viewModel.loadDashboardData()
    }
    
    private fun animateViews() {
        // Get all card views from the layout
        val cards = mutableListOf<View>()
        
        // Find all CardViews in the layout
        binding.root.findViewsWithType(androidx.cardview.widget.CardView::class.java, cards)
        
        // Animate cards in sequence with slide up effect
        AnimationUtils.animateSequence(cards, AnimationUtils.AnimationType.SLIDE_UP, 100)
    }
    
    private fun View.findViewsWithType(type: Class<*>, result: MutableList<View>) {
        if (type.isInstance(this)) {
            result.add(this)
        }
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).findViewsWithType(type, result)
            }
        }
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
                        // Display user's own name or email
                        val userName = profile.user.name?.takeIf { it.isNotBlank() } ?: profile.user.email
                        binding.tvEmail.text = userName
                        
                        binding.tvRole.text = "User Administrator"
                        
                        // Display company name
                        binding.tvCompany.text = profile.hierarchy?.companyName ?: "—"
                        
                        // Display client admin info
                        profile.hierarchy?.clientAdmin?.let { admin ->
                            val adminName = admin.name?.takeIf { it.isNotBlank() } ?: admin.email
                            binding.tvClientAdmin.text = adminName
                        } ?: run {
                            binding.tvClientAdmin.text = "—"
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
        
        viewModel.summaryState.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { summary ->
                        summary.displays?.let { displays ->
                            binding.tvTotalDisplays.text = displays.total.toString()
                            binding.tvOnlineDisplays.text = "${displays.online} online"
                            binding.tvOfflineDisplays.text = "${displays.offline} offline"
                        }
                        
                        binding.tvMediaCount.text = summary.mediaCount?.toString() ?: "0"
                        binding.tvPlaylistCount.text = summary.playlistCount?.toString() ?: "0"
                        
                        summary.storageBytes?.let { bytes ->
                            binding.tvStorage.text = formatBytes(bytes)
                        } ?: run {
                            binding.tvStorage.text = "0 B"
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
    
    private fun formatBytes(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
        val i = Math.floor(Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, i.toDouble())
        return String.format("%.1f %s", value, sizes[i])
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
