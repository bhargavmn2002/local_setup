package com.signox.dashboard.ui.playlist

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
import com.signox.dashboard.data.model.Playlist
import com.signox.dashboard.databinding.FragmentPlaylistListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistListFragment : Fragment() {
    
    private var _binding: FragmentPlaylistListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var adapter: PlaylistAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load playlists
        viewModel.loadPlaylists()
    }
    
    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                navigateToPlaylistEdit(playlist.id)
            },
            onDeleteClick = { playlist ->
                showDeleteConfirmation(playlist)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PlaylistListFragment.adapter
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupFab() {
        binding.fabCreatePlaylist.setOnClickListener {
            navigateToCreatePlaylist()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPlaylists()
        }
    }
    
    private fun observeViewModel() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            updateUI(playlists)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading && adapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }
    }
    
    private fun updateUI(playlists: List<Playlist>) {
        if (playlists.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            adapter.submitList(playlists)
        }
    }
    
    private fun navigateToCreatePlaylist() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PlaylistCreateFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToPlaylistEdit(playlistId: String) {
        val fragment = PlaylistEditFragment().apply {
            arguments = Bundle().apply {
                putString("playlistId", playlistId)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun showDeleteConfirmation(playlist: Playlist) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete \"${playlist.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlaylist(playlist.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
