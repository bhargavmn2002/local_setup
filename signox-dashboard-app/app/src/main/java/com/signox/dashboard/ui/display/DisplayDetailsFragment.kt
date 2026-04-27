package com.signox.dashboard.ui.display

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.databinding.FragmentDisplayDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DisplayDetailsFragment : Fragment() {
    
    private var _binding: FragmentDisplayDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DisplayViewModel by viewModels()
    private var display: Display? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get display from arguments
        display = arguments?.getParcelable("display")
        
        setupToolbar()
        setupButtons()
        observeViewModel()
        
        // Load display details
        display?.let {
            viewModel.loadDisplay(it.id)
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupButtons() {
        binding.btnEditDisplay.setOnClickListener {
            // Navigate to edit screen (to be implemented in future)
            Toast.makeText(requireContext(), "Edit feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnAssignPlaylist.setOnClickListener {
            showPlaylistSelectionDialog()
        }
        
        binding.btnUnassignPlaylist.setOnClickListener {
            unassignPlaylist()
        }
        
        binding.btnAssignLayout.setOnClickListener {
            showLayoutSelectionDialog()
        }
        
        binding.btnUnassignLayout.setOnClickListener {
            unassignLayout()
        }
        
        binding.btnDeleteDisplay.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun observeViewModel() {
        // Display details
        viewModel.selectedDisplay.observe(viewLifecycleOwner) { display ->
            display?.let {
                updateUI(it)
            }
        }
        
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnEditDisplay.isEnabled = !isLoading
            binding.btnDeleteDisplay.isEnabled = !isLoading
        }
        
        // Error messages
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Success messages (for delete)
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
                // Navigate back after successful deletion
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    private fun updateUI(display: Display) {
        binding.apply {
            // Display name
            tvDisplayName.text = display.name
            
            // Status
            val statusColor = when (display.status.uppercase()) {
                "ONLINE" -> R.color.status_online
                "OFFLINE" -> R.color.status_offline
                "PAIRING" -> R.color.status_pairing
                else -> R.color.status_error
            }
            viewStatusIndicator.setBackgroundColor(
                ContextCompat.getColor(requireContext(), statusColor)
            )
            tvStatus.text = display.status
            tvStatus.setTextColor(
                ContextCompat.getColor(requireContext(), statusColor)
            )
            
            // Display ID
            tvDisplayId.text = display.id.take(8) + "..."
            
            // Location
            tvLocation.text = display.location ?: "Not set"
            
            // Last seen
            if (display.lastSeenAt != null) {
                tvLastSeen.text = formatDate(display.lastSeenAt)
            } else {
                tvLastSeen.text = "Never"
            }
            
            // Paired at
            if (display.pairedAt != null) {
                tvPairedAt.text = formatDate(display.pairedAt)
            } else {
                tvPairedAt.text = "Unknown"
            }
            
            // Active schedule
            if (display.activeSchedule != null) {
                layoutActiveSchedule.visibility = View.VISIBLE
                tvActiveSchedule.text = display.activeSchedule.name
            } else {
                layoutActiveSchedule.visibility = View.GONE
            }
            
            // Playlist
            tvPlaylist.text = display.playlist?.name ?: "None"
            
            // Layout
            tvLayout.text = display.layout?.name ?: "None"
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Display")
            .setMessage("Are you sure you want to delete this display? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                display?.let {
                    viewModel.deleteDisplay(it.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPlaylistSelectionDialog() {
        // Get playlists from PlaylistViewModel
        val playlistViewModel: com.signox.dashboard.ui.playlist.PlaylistViewModel by viewModels()
        playlistViewModel.loadPlaylists()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_selection, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.playlistsRecyclerView)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchInput)
        val loadingIndicator = dialogView.findViewById<android.widget.ProgressBar>(R.id.loadingIndicator)
        val emptyState = dialogView.findViewById<android.widget.TextView>(R.id.emptyState)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val assignButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.assignButton)
        
        val playlistAdapter = com.signox.dashboard.ui.display.PlaylistSelectionAdapter { selectedId ->
            assignButton?.isEnabled = selectedId != null
        }
        
        recyclerView?.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = playlistAdapter
        }
        
        dialog.show()
        
        // Observe playlists
        playlistViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            if (playlists.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyState?.visibility = View.GONE
                playlistAdapter.submitList(playlists)
            }
        }
        
        playlistViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            assignButton?.isEnabled = !isLoading && playlistAdapter.getSelectedPlaylistId() != null
        }
        
        // Search functionality
        searchInput?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                val allPlaylists = playlistViewModel.playlists.value ?: emptyList()
                val filtered = if (query.isEmpty()) {
                    allPlaylists
                } else {
                    allPlaylists.filter { it.name.lowercase().contains(query) }
                }
                playlistAdapter.submitList(filtered)
            }
        })
        
        // Cancel button
        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }
        
        // Assign button
        assignButton?.setOnClickListener {
            val selectedPlaylistId = playlistAdapter.getSelectedPlaylistId()
            if (selectedPlaylistId != null && display != null) {
                assignButton.isEnabled = false
                viewModel.updateDisplay(
                    id = display!!.id,
                    playlistId = selectedPlaylistId
                )
                dialog.dismiss()
            }
        }
    }
    
    private fun unassignPlaylist() {
        if (display == null) return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Unassign Playlist")
            .setMessage("Are you sure you want to unassign the playlist from this display?")
            .setPositiveButton("Unassign") { _, _ ->
                viewModel.updateDisplay(
                    id = display!!.id,
                    playlistId = "" // Empty string to disconnect
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLayoutSelectionDialog() {
        // Get layouts from LayoutViewModel
        val layoutViewModel: com.signox.dashboard.ui.layout.LayoutViewModel by viewModels()
        layoutViewModel.loadLayouts()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_layout_selection, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.layoutsRecyclerView)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchInput)
        val loadingIndicator = dialogView.findViewById<android.widget.ProgressBar>(R.id.loadingIndicator)
        val emptyState = dialogView.findViewById<android.widget.TextView>(R.id.emptyState)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val assignButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.assignButton)
        
        val layoutAdapter = LayoutSelectionAdapter { selectedId ->
            assignButton?.isEnabled = selectedId != null
        }
        
        recyclerView?.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = layoutAdapter
        }
        
        dialog.show()
        
        // Observe layouts
        layoutViewModel.layouts.observe(viewLifecycleOwner) { layouts ->
            if (layouts.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyState?.visibility = View.GONE
                layoutAdapter.submitList(layouts)
            }
        }
        
        layoutViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            assignButton?.isEnabled = !isLoading && layoutAdapter.getSelectedLayoutId() != null
        }
        
        // Search functionality
        searchInput?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                val allLayouts = layoutViewModel.layouts.value ?: emptyList()
                val filtered = if (query.isEmpty()) {
                    allLayouts
                } else {
                    allLayouts.filter { it.name.lowercase().contains(query) }
                }
                layoutAdapter.submitList(filtered)
            }
        })
        
        // Cancel button
        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }
        
        // Assign button
        assignButton?.setOnClickListener {
            val selectedLayoutId = layoutAdapter.getSelectedLayoutId()
            if (selectedLayoutId != null && display != null) {
                assignButton.isEnabled = false
                viewModel.updateDisplay(
                    id = display!!.id,
                    layoutId = selectedLayoutId
                )
                dialog.dismiss()
            }
        }
    }
    
    private fun unassignLayout() {
        if (display == null) return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Unassign Layout")
            .setMessage("Are you sure you want to unassign the layout from this display?")
            .setPositiveButton("Unassign") { _, _ ->
                viewModel.updateDisplay(
                    id = display!!.id,
                    layoutId = "" // Empty string to disconnect
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)
            
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hours ago"
                days < 7 -> "$days days ago"
                else -> {
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    outputFormat.format(date!!)
                }
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSelectedDisplay()
        _binding = null
    }
}
