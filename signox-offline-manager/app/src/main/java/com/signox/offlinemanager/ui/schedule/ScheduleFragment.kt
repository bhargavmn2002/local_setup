package com.signox.offlinemanager.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.offlinemanager.R
import com.signox.offlinemanager.SignoXOfflineApplication
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository
import com.signox.offlinemanager.databinding.FragmentScheduleBinding

class ScheduleFragment : Fragment() {
    
    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ScheduleViewModel by viewModels {
        val app = requireActivity().application as SignoXOfflineApplication
        val scheduleRepository = ScheduleRepository(app.database.scheduleDao())
        val playlistRepository = PlaylistRepository(app.database.playlistDao())
        val layoutRepository = LayoutRepository(app.database.layoutDao())
        ScheduleViewModelFactory(scheduleRepository, playlistRepository, layoutRepository)
    }
    
    private lateinit var scheduleAdapter: ScheduleAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onScheduleClick = { schedule -> showScheduleDetails(schedule) },
            onEditClick = { schedule -> showEditScheduleDialog(schedule) },
            onToggleStatus = { schedule -> viewModel.toggleScheduleStatus(schedule) },
            onDeleteClick = { schedule -> showDeleteConfirmation(schedule) },
            formatDaysOfWeek = { daysOfWeek -> viewModel.formatDaysOfWeek(daysOfWeek) },
            getContentName = { contentType, contentId -> viewModel.getContentName(contentType, contentId) }
        )
        
        binding.recyclerViewSchedules.apply {
            adapter = scheduleAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSearchView() {
        binding.searchViewSchedules.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupFab() {
        binding.fabCreateSchedule.setOnClickListener {
            showCreateScheduleDialog()
        }
    }
    
    private fun observeViewModel() {
        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            val filteredSchedules = filterSchedules(schedules, viewModel.searchQuery.value ?: "")
            scheduleAdapter.submitList(filteredSchedules)
            
            binding.layoutEmptyState.visibility = if (schedules.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            val schedules = viewModel.schedules.value ?: emptyList()
            val filteredSchedules = filterSchedules(schedules, query)
            scheduleAdapter.submitList(filteredSchedules)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        viewModel.conflictError.observe(viewLifecycleOwner) { error ->
            error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Schedule Conflict")
                    .setMessage(it)
                    .setPositiveButton("OK", null)
                    .show()
                viewModel.clearConflictError()
            }
        }
    }
    
    private fun filterSchedules(schedules: List<Schedule>, query: String): List<Schedule> {
        if (query.isEmpty()) return schedules
        
        return schedules.filter { schedule ->
            schedule.name.contains(query, ignoreCase = true) ||
            schedule.description?.contains(query, ignoreCase = true) == true ||
            viewModel.getContentName(schedule.contentType, schedule.contentId).contains(query, ignoreCase = true)
        }
    }
    
    private fun showCreateScheduleDialog() {
        val playlists = viewModel.playlists.value ?: emptyList()
        val layouts = viewModel.layouts.value ?: emptyList()
        
        if (playlists.isEmpty() && layouts.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please create playlists or layouts first",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        val dialog = CreateScheduleDialog(
            onCreateSchedule = { name, description, startTime, endTime, daysOfWeek, contentType, contentId ->
                viewModel.createSchedule(name, description, startTime, endTime, daysOfWeek, contentType, contentId)
            },
            playlists = playlists,
            layouts = layouts
        )
        dialog.show(parentFragmentManager, "CreateScheduleDialog")
    }
    
    private fun showEditScheduleDialog(schedule: Schedule) {
        val playlists = viewModel.playlists.value ?: emptyList()
        val layouts = viewModel.layouts.value ?: emptyList()
        
        val dialog = CreateScheduleDialog(
            onCreateSchedule = { name, description, startTime, endTime, daysOfWeek, contentType, contentId ->
                viewModel.updateSchedule(
                    schedule.id, name, description, startTime, endTime, 
                    daysOfWeek, contentType, contentId, schedule.isActive
                )
            },
            playlists = playlists,
            layouts = layouts,
            existingSchedule = schedule
        )
        dialog.show(parentFragmentManager, "EditScheduleDialog")
    }
    
    private fun showScheduleDetails(schedule: Schedule) {
        val contentName = viewModel.getContentName(schedule.contentType, schedule.contentId)
        val daysText = viewModel.formatDaysOfWeek(schedule.daysOfWeek)
        val statusText = if (schedule.isActive) "Active" else "Inactive"
        
        val message = """
            Time: ${schedule.startTime} - ${schedule.endTime}
            Days: $daysText
            Content: ${schedule.contentType.name.lowercase().replaceFirstChar { it.uppercase() }}: $contentName
            Status: $statusText
            
            ${schedule.description ?: "No description"}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(schedule.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showDeleteConfirmation(schedule: Schedule) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete \"${schedule.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSchedule(schedule)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}