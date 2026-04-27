package com.signox.dashboard.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.databinding.FragmentPlaylistCreateBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistCreateFragment : Fragment() {
    
    private var _binding: FragmentPlaylistCreateBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlaylistViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistCreateBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupButtons()
        observeViewModel()
    }
    
    private fun setupButtons() {
        binding.buttonCreate.setOnClickListener {
            createPlaylist()
        }
        
        binding.buttonCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun createPlaylist() {
        val name = binding.editTextPlaylistName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.editTextPlaylistName.error = "Playlist name is required"
            return
        }
        
        viewModel.createPlaylist(name)
    }
    
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonCreate.isEnabled = !isLoading
            binding.buttonCancel.isEnabled = !isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        viewModel.currentPlaylist.observe(viewLifecycleOwner) { playlist ->
            playlist?.let {
                // Navigate to edit screen to add items
                val fragment = PlaylistEditFragment().apply {
                    arguments = Bundle().apply {
                        putString("playlistId", it.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(com.signox.dashboard.R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
                viewModel.clearCurrentPlaylist()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
