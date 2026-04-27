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
import com.signox.dashboard.databinding.FragmentProofOfPlayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProofOfPlayFragment : Fragment() {
    
    private var _binding: FragmentProofOfPlayBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsViewModel by viewModels()
    private lateinit var playbackLogAdapter: PlaybackLogAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProofOfPlayBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupRefresh()
        
        // Load initial data
        viewModel.loadProofOfPlay()
    }
    
    private fun setupRecyclerView() {
        playbackLogAdapter = PlaybackLogAdapter()
        binding.rvPlaybackLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playbackLogAdapter
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.proofOfPlayLogs.collect { logs ->
                if (logs.isNotEmpty()) {
                    binding.rvPlaybackLogs.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    playbackLogAdapter.submitList(logs)
                } else {
                    binding.rvPlaybackLogs.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
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
            viewModel.loadProofOfPlay()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
