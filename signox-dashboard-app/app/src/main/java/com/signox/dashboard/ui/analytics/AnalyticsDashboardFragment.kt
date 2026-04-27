package com.signox.dashboard.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.databinding.FragmentAnalyticsDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsDashboardFragment : Fragment() {
    
    private var _binding: FragmentAnalyticsDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsViewModel by viewModels()
    private lateinit var contentStatsAdapter: ContentStatsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupRefresh()
        
        // Load initial data
        viewModel.loadAnalyticsSummary()
    }
    
    private fun setupRecyclerView() {
        contentStatsAdapter = ContentStatsAdapter()
        binding.rvMostPlayed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentStatsAdapter
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.analyticsSummary.collect { summary ->
                summary?.let { updateUI(it) }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadAnalyticsSummary()
        }
    }
    
    private fun updateUI(summary: com.signox.dashboard.data.model.AnalyticsSummary) {
        binding.apply {
            // Display stats
            tvTotalDisplays.text = summary.totalDisplays.toString()
            tvOnlineDisplays.text = summary.onlineDisplays.toString()
            tvOfflineDisplays.text = summary.offlineDisplays.toString()
            
            // Content stats
            tvTotalContent.text = summary.totalContent.toString()
            tvTotalPlaylists.text = summary.totalPlaylists.toString()
            
            // Uptime
            tvAverageUptime.text = String.format("%.1f%%", summary.averageUptime)
            
            // Playback time
            val hours = summary.totalPlaybackTime / 3600
            tvTotalPlaybackTime.text = "$hours hrs"
            
            // Most played content
            summary.mostPlayedContent?.let { content ->
                if (content.isNotEmpty()) {
                    rvMostPlayed.visibility = View.VISIBLE
                    tvNoContent.visibility = View.GONE
                    contentStatsAdapter.submitList(content)
                } else {
                    rvMostPlayed.visibility = View.GONE
                    tvNoContent.visibility = View.VISIBLE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
