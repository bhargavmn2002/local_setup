package com.signox.dashboard.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.data.model.Playlist
import com.signox.dashboard.data.model.PlaylistItem
import com.signox.dashboard.data.model.PlaylistItemRequest
import com.signox.dashboard.databinding.FragmentPlaylistEditBinding
import com.signox.dashboard.ui.media.MediaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistEditFragment : Fragment() {
    
    private var _binding: FragmentPlaylistEditBinding? = null
    private val binding get() = _binding!!
    
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()
    
    private lateinit var itemsAdapter: PlaylistItemsAdapter
    private var playlistId: String? = null
    private var currentPlaylist: Playlist? = null
    private var displayDialog: androidx.appcompat.app.AlertDialog? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistEditBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playlistId = arguments?.getString("playlistId")
        
        setupRecyclerView()
        setupButtons()
        observeViewModels()
        
        // Load playlist and media
        playlistId?.let { playlistViewModel.loadPlaylist(it) }
        
        // Load all media without filters for selection
        mediaViewModel.setFilterType("all")
        mediaViewModel.setSearchQuery("")
        mediaViewModel.loadMedia(refresh = true)
    }
    
    private fun setupRecyclerView() {
        itemsAdapter = PlaylistItemsAdapter(
            onRemoveClick = { item ->
                itemsAdapter.removeItem(item)
                updateTotalDuration()
            },
            onDurationChange = { item, duration ->
                itemsAdapter.updateItemDuration(item, duration)
                updateTotalDuration()
            }
        )
        
        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemsAdapter
        }
        
        // Enable drag and drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                itemsAdapter.moveItem(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewItems)
    }
    
    private fun setupButtons() {
        binding.buttonAddMedia.setOnClickListener {
            showMediaSelectionDialog()
        }
        
        binding.buttonPreview.setOnClickListener {
            showPreview()
        }
        
        binding.buttonSave.setOnClickListener {
            savePlaylist()
        }
        
        binding.buttonCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun observeViewModels() {
        playlistViewModel.currentPlaylist.observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                currentPlaylist = it
                binding.editTextPlaylistName.setText(it.name)
                itemsAdapter.submitList(it.items ?: emptyList())
                updateTotalDuration()
                updateEmptyState()
            }
        }
        
        playlistViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
        }
        
        playlistViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                playlistViewModel.clearError()
            }
        }
        
        playlistViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                playlistViewModel.clearSuccessMessage()
                // Don't pop back stack for assignment success
                if (it.contains("updated successfully")) {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        
        playlistViewModel.assignmentSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Reload displays to show updated assignments
                playlistViewModel.loadDisplays()
                // Dismiss the dialog if it's showing
                dismissDisplayDialog()
            }
        }
        
        // Observe media loading
        mediaViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Disable add media button while loading
            binding.buttonAddMedia.isEnabled = !isLoading
            if (isLoading) {
                binding.buttonAddMedia.text = "Loading Media..."
            } else {
                binding.buttonAddMedia.text = "Add Media"
            }
        }
        
        mediaViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Media error: $it", Toast.LENGTH_SHORT).show()
                mediaViewModel.clearError()
            }
        }
    }
    
    private fun showMediaSelectionDialog() {
        // Check if media is still loading
        if (mediaViewModel.isLoading.value == true) {
            Toast.makeText(requireContext(), "Loading media, please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        
        val mediaList = mediaViewModel.mediaList.value ?: emptyList()
        
        if (mediaList.isEmpty()) {
            Toast.makeText(requireContext(), "No media available. Upload media first.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val mediaNames = mediaList.map { "${it.name} (${it.type})" }.toTypedArray()
        val selectedItems = mutableListOf<Media>()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Media")
            .setMultiChoiceItems(mediaNames, null) { _, which, isChecked ->
                if (isChecked) {
                    selectedItems.add(mediaList[which])
                } else {
                    selectedItems.remove(mediaList[which])
                }
            }
            .setPositiveButton("Add") { _, _ ->
                addMediaItems(selectedItems)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addMediaItems(mediaList: List<Media>) {
        itemsAdapter.addMediaItems(mediaList)
        updateTotalDuration()
        updateEmptyState()
    }
    
    private fun updateTotalDuration() {
        val totalSeconds = itemsAdapter.getTotalDuration()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        
        val durationText = when {
            hours > 0 -> String.format("%dh %dm", hours, minutes % 60)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
        
        binding.textTotalDuration.text = "Total Duration: $durationText"
    }
    
    private fun updateEmptyState() {
        if (itemsAdapter.itemCount == 0) {
            binding.recyclerViewItems.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerViewItems.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }
    
    private fun savePlaylist() {
        val name = binding.editTextPlaylistName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.editTextPlaylistName.error = "Playlist name is required"
            return
        }
        
        val items = itemsAdapter.getPlaylistItems()
        
        playlistId?.let {
            playlistViewModel.updatePlaylist(it, name, items)
        }
    }
    
    private fun showDisplaySelectionDialog() {
        // Load displays first
        playlistViewModel.loadDisplays()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_display_selection, null)
        displayDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.displaysRecyclerView)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchInput)
        val loadingIndicator = dialogView.findViewById<ProgressBar>(R.id.loadingIndicator)
        val emptyState = dialogView.findViewById<TextView>(R.id.emptyState)
        val selectedCount = dialogView.findViewById<TextView>(R.id.selectedCount)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val assignButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.assignButton)
        
        val displayAdapter = DisplaySelectionAdapter { selectedIds ->
            selectedCount?.text = "${selectedIds.size} display(s) selected"
            assignButton?.isEnabled = selectedIds.isNotEmpty()
        }
        
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = displayAdapter
        }
        
        // Show dialog first
        displayDialog?.show()
        
        // Then observe displays
        val displaysObserver = androidx.lifecycle.Observer<List<com.signox.dashboard.data.model.Display>> { displays ->
            if (displays.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyState?.visibility = View.GONE
                displayAdapter.submitList(displays)
            }
        }
        
        val loadingObserver = androidx.lifecycle.Observer<Boolean> { isLoading ->
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            assignButton?.isEnabled = !isLoading && displayAdapter.getSelectedDisplayIds().isNotEmpty()
        }
        
        playlistViewModel.displays.observe(viewLifecycleOwner, displaysObserver)
        playlistViewModel.isLoading.observe(viewLifecycleOwner, loadingObserver)
        
        // Clean up observers when dialog is dismissed
        displayDialog?.setOnDismissListener {
            playlistViewModel.displays.removeObserver(displaysObserver)
            playlistViewModel.isLoading.removeObserver(loadingObserver)
        }
        
        // Search functionality
        searchInput?.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val allDisplays = playlistViewModel.displays.value ?: emptyList()
            val filtered = if (query.isEmpty()) {
                allDisplays
            } else {
                allDisplays.filter { it.name.lowercase().contains(query) }
            }
            displayAdapter.submitList(filtered)
        }
        
        // Cancel button
        cancelButton?.setOnClickListener {
            displayDialog?.dismiss()
        }
        
        // Assign button
        assignButton?.setOnClickListener {
            val selectedIds = displayAdapter.getSelectedDisplayIds()
            if (selectedIds.isNotEmpty() && playlistId != null) {
                // Disable button to prevent double-click
                assignButton.isEnabled = false
                playlistViewModel.assignPlaylistToDisplays(playlistId!!, selectedIds)
                // Don't dismiss immediately - wait for success/failure
            }
        }
    }
    
    private fun dismissDisplayDialog() {
        displayDialog?.dismiss()
        displayDialog = null
    }
    
    private fun showPreview() {
        if (playlistId == null) {
            Toast.makeText(requireContext(), "Please save the playlist first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (itemsAdapter.itemCount == 0) {
            Toast.makeText(requireContext(), "Add items to preview the playlist", Toast.LENGTH_SHORT).show()
            return
        }
        
        val fragment = PlaylistPreviewFragment().apply {
            arguments = Bundle().apply {
                putString("playlistId", playlistId)
            }
        }
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
