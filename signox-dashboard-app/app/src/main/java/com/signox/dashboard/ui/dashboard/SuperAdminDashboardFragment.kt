package com.signox.dashboard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.databinding.FragmentSuperAdminDashboardBinding
import com.signox.dashboard.utils.AnimationUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuperAdminDashboardFragment : Fragment() {
    
    private var _binding: FragmentSuperAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuperAdminDashboardBinding.inflate(inflater, container, false)
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
                        binding.tvEmail.text = profile.user.email
                        binding.tvRole.text = "Super Administrator"
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
                        binding.tvTotalClients.text = summary.totalClients?.toString() ?: "—"
                        binding.tvTotalDisplays.text = summary.totalDisplays?.toString() ?: "—"
                        binding.tvOnlineDisplays.text = summary.onlineDisplays?.toString() ?: "—"
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
