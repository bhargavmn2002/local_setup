package com.signox.dashboard.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.databinding.FragmentScheduleListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleListFragment : Fragment() {
    
    private var _binding: FragmentScheduleListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ScheduleViewModel by viewModels()
    private lateinit var scheduleAdapter: ScheduleAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load schedules
        viewModel.loadSchedules()
    }
    
    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onScheduleClick = { schedule ->
                // Navigate to schedule details/edit
                val fragment = ScheduleEditorFragment.newInstance(schedule.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onToggleActive = { schedule ->
                viewModel.toggleScheduleActive(schedule)
            },
            onDeleteClick = { schedule ->
                showDeleteConfirmation(schedule)
            }
        )
        
        binding.schedulesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchSchedules(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupFab() {
        binding.fab.setOnClickListener {
            // Navigate to create schedule
            val fragment = ScheduleEditorFragment.newInstance(null)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSchedules()
        }
    }
    
    private fun observeViewModel() {
        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            scheduleAdapter.submitList(schedules)
            binding.emptyState.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
                viewModel.loadSchedules() // Reload after success
            }
        }
    }
    
    private fun showDeleteConfirmation(schedule: com.signox.dashboard.data.model.Schedule) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete \"${schedule.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSchedule(schedule.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = ScheduleListFragment()
    }
}
