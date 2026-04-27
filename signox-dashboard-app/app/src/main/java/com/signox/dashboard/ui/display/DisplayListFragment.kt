package com.signox.dashboard.ui.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.databinding.FragmentDisplayListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayListFragment : Fragment() {
    
    private var _binding: FragmentDisplayListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DisplayViewModel by viewModels()
    private lateinit var displayAdapter: DisplayAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user role
        val sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("user_role", null)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupSwipeRefresh()
        setupFab(userRole)
        observeViewModel()
        
        // Load displays
        viewModel.loadDisplays()
    }
    
    private fun setupRecyclerView() {
        displayAdapter = DisplayAdapter { display ->
            // Navigate to display details
            val fragment = DisplayDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("display", display)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.rvDisplays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = displayAdapter
        }
    }
    
    private fun setupSearchAndFilters() {
        // Search
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
        
        // Filter chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipOnline -> "online"
                R.id.chipOffline -> "offline"
                else -> "all"
            }
            viewModel.setFilterStatus(status)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDisplays()
        }
    }
    
    private fun setupFab(userRole: String?) {
        // CLIENT_ADMIN can only view displays, not add them
        if (userRole == "CLIENT_ADMIN") {
            binding.fabPairDisplay.visibility = View.GONE
        } else {
            binding.fabPairDisplay.visibility = View.VISIBLE
            binding.fabPairDisplay.setOnClickListener {
                // Navigate to pairing screen
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DisplayPairingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
    
    private fun observeViewModel() {
        // Filtered displays
        viewModel.filteredDisplays.observe(viewLifecycleOwner) { displays ->
            displayAdapter.submitList(displays)
            
            // Show/hide empty state
            if (displays.isEmpty()) {
                binding.rvDisplays.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvDisplays.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            }
        }
        
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading && displayAdapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        // Error messages
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Success messages
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Start monitoring when fragment is visible
        viewModel.startMonitoring()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop monitoring when fragment is not visible
        viewModel.stopMonitoring()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
