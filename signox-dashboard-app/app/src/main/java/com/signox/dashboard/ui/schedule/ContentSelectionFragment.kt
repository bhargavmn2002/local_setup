package com.signox.dashboard.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.signox.dashboard.databinding.FragmentContentSelectionBinding
import com.signox.dashboard.ui.layout.LayoutViewModel
import com.signox.dashboard.ui.playlist.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentSelectionFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentContentSelectionBinding? = null
    private val binding get() = _binding!!
    
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val layoutViewModel: LayoutViewModel by viewModels()
    
    private lateinit var contentAdapter: ContentRadioAdapter
    
    private var onContentSelected: ((type: String, id: String, name: String) -> Unit)? = null
    private var selectedType: String = "playlist"
    private var selectedId: String? = null
    private var selectedName: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupRecyclerView()
        setupButtons()
        observeViewModels()
        
        // Load playlists by default
        playlistViewModel.loadPlaylists()
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Playlists"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Layouts"))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        selectedType = "playlist"
                        playlistViewModel.loadPlaylists()
                    }
                    1 -> {
                        selectedType = "layout"
                        layoutViewModel.loadLayouts()
                    }
                }
                selectedId = null
                selectedName = null
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        contentAdapter = ContentRadioAdapter { id, name ->
            selectedId = id
            selectedName = name
        }
        
        binding.contentRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentAdapter
        }
    }
    
    private fun setupButtons() {
        binding.confirmButton.setOnClickListener {
            if (selectedId == null) {
                Toast.makeText(requireContext(), "Please select content", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onContentSelected?.invoke(selectedType, selectedId!!, selectedName!!)
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun observeViewModels() {
        playlistViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            if (selectedType == "playlist") {
                val items = playlists.map { ContentItem(it.id, it.name) }
                contentAdapter.submitList(items)
                binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        layoutViewModel.layouts.observe(viewLifecycleOwner) { layouts ->
            if (selectedType == "layout") {
                val items = layouts.map { ContentItem(it.id, it.name) }
                contentAdapter.submitList(items)
                binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        playlistViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (selectedType == "playlist") {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        layoutViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (selectedType == "layout") {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(
            onSelected: (type: String, id: String, name: String) -> Unit
        ) = ContentSelectionFragment().apply {
            this.onContentSelected = onSelected
        }
    }
}

data class ContentItem(
    val id: String,
    val name: String
)
