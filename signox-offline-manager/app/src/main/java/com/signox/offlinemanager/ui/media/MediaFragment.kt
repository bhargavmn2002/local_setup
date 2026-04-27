package com.signox.offlinemanager.ui.media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.databinding.FragmentMediaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaFragment : Fragment() {
    
    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MediaViewModel
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var mediaImportHelper: MediaImportHelper
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importMediaFile(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
        
        mediaImportHelper = MediaImportHelper(requireContext())
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MediaViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter(
            onPreviewClick = { media -> showMediaPreview(media) },
            onDeleteClick = { media -> confirmDeleteMedia(media) }
        )
        
        binding.rvMedia.apply {
            adapter = mediaAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnImportMedia.setOnClickListener {
            openFilePicker()
        }
        
        binding.btnFilter.setOnClickListener {
            showFilterMenu()
        }
    }
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchMedia(s?.toString() ?: "")
            }
        })
    }
    
    private fun observeViewModel() {
        viewModel.filteredMediaList.observe(viewLifecycleOwner) { mediaList ->
            mediaAdapter.submitList(mediaList)
            updateEmptyState(mediaList.isEmpty())
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
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvMedia.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mediaImportHelper.getSupportedMimeTypes())
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Media File"))
    }
    
    private fun importMediaFile(uri: android.net.Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val media = withContext(Dispatchers.IO) {
                    mediaImportHelper.importMedia(uri)
                }
                
                if (media != null) {
                    viewModel.addMedia(media)
                    Toast.makeText(requireContext(), "Media imported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to import media file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error importing media: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showFilterMenu() {
        val popup = PopupMenu(requireContext(), binding.btnFilter)
        popup.menuInflater.inflate(R.menu.menu_media_filter, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter_all -> {
                    viewModel.filterByType(MediaType.values().toList())
                    binding.btnFilter.text = "All"
                    true
                }
                R.id.filter_images -> {
                    viewModel.filterByType(listOf(MediaType.IMAGE))
                    binding.btnFilter.text = "Images"
                    true
                }
                R.id.filter_videos -> {
                    viewModel.filterByType(listOf(MediaType.VIDEO))
                    binding.btnFilter.text = "Videos"
                    true
                }
                R.id.filter_audio -> {
                    viewModel.filterByType(listOf(MediaType.AUDIO))
                    binding.btnFilter.text = "Audio"
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showMediaPreview(media: Media) {
        val dialog = MediaPreviewDialog(
            requireContext(),
            media
        ) { mediaToDelete ->
            confirmDeleteMedia(mediaToDelete)
        }
        dialog.show()
    }
    
    private fun confirmDeleteMedia(media: Media) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Media")
            .setMessage("Are you sure you want to delete '${media.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMedia(media)
                Toast.makeText(requireContext(), "Media deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}