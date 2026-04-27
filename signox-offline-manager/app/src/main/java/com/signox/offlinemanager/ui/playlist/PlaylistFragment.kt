package com.signox.offlinemanager.ui.playlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.databinding.FragmentPlaylistBinding

class PlaylistFragment : Fragment() {
    
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PlaylistViewModel
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val mediaRepository = MediaRepository(database.mediaDao())
        
        val factory = PlaylistViewModelFactory(playlistRepository, mediaRepository)
        viewModel = ViewModelProvider(this, factory)[PlaylistViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlistWithDetails ->
                openPlaylistEditor(playlistWithDetails.playlist.id)
            },
            onEditClick = { playlistWithDetails ->
                showEditPlaylistDialog(playlistWithDetails)
            },
            onDeleteClick = { playlistWithDetails ->
                showDeleteConfirmation(playlistWithDetails)
            },
            onPreviewClick = { playlistWithDetails ->
                // TODO: Implement playlist preview
                Toast.makeText(requireContext(), "Preview coming soon", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PlaylistFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPlaylist.setOnClickListener {
            android.util.Log.d("PlaylistFragment", "FAB clicked")
            Toast.makeText(requireContext(), "Opening playlist dialog...", Toast.LENGTH_SHORT).show()
            showCreatePlaylistDialog()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchPlaylists(text.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.filteredPlaylists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
            updateEmptyState(playlists.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show/hide loading indicator if needed
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPlaylists.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showCreatePlaylistDialog() {
        android.util.Log.d("PlaylistFragment", "Opening create playlist dialog")
        CreatePlaylistDialog(
            requireContext(),
            onPlaylistSaved = { name, description ->
                android.util.Log.d("PlaylistFragment", "Playlist saved callback: name='$name', description='$description'")
                viewModel.createPlaylist(name, description)
            }
        ).show()
    }

    private fun showEditPlaylistDialog(playlistWithDetails: PlaylistViewModel.PlaylistWithDetails) {
        CreatePlaylistDialog(
            requireContext(),
            playlist = playlistWithDetails.playlist,
            onPlaylistSaved = { name, description ->
                val updatedPlaylist = playlistWithDetails.playlist.copy(
                    name = name,
                    description = description
                )
                viewModel.updatePlaylist(updatedPlaylist)
            }
        ).show()
    }

    private fun showDeleteConfirmation(playlistWithDetails: PlaylistViewModel.PlaylistWithDetails) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete \"${playlistWithDetails.playlist.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlaylist(playlistWithDetails.playlist.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openPlaylistEditor(playlistId: Long) {
        val intent = Intent(requireContext(), PlaylistEditorActivity::class.java)
        intent.putExtra("playlist_id", playlistId)
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}