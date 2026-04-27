package com.signox.dashboard.ui.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.databinding.FragmentMediaListBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MediaListFragment : Fragment() {
    
    private var _binding: FragmentMediaListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MediaViewModel by viewModels()
    private lateinit var mediaAdapter: MediaAdapter
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileSelection(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupFab()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load initial data
        viewModel.loadMedia(refresh = true)
        viewModel.loadStorageInfo()
    }
    
    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter(
            onMediaClick = { media ->
                // Open media preview
                val fragment = MediaPreviewFragment.newInstance(media)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onMediaLongClick = { media ->
                showMediaOptionsDialog(media)
            }
        )
        
        binding.rvMedia.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = mediaAdapter
            
            // Apply layout animation
            com.signox.dashboard.utils.AnimationUtils.applyRecyclerViewAnimation(this)
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        viewModel.loadMoreMedia()
                    }
                }
            })
        }
    }
    
    private fun setupSearchAndFilters() {
        // Search
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
        
        // Filter chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val filterType = if (checkedIds.isEmpty()) {
                "all"
            } else {
                val selectedChip = group.findViewById<Chip>(checkedIds.first())
                when (selectedChip?.id) {
                    R.id.chip_images -> "image"
                    R.id.chip_videos -> "video"
                    else -> "all"
                }
            }
            viewModel.setFilterType(filterType)
        }
    }
    
    private fun setupFab() {
        binding.fabUpload.setOnClickListener {
            // Add pulse animation on FAB click
            com.signox.dashboard.utils.AnimationUtils.pulse(binding.fabUpload)
            openFilePicker()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMedia(refresh = true)
            viewModel.loadStorageInfo()
        }
    }
    
    private fun observeViewModel() {
        viewModel.mediaList.observe(viewLifecycleOwner) { mediaList ->
            android.util.Log.d("MediaListFragment", "mediaList updated: ${mediaList.size} items")
        }
        
        viewModel.filteredMedia.observe(viewLifecycleOwner) { mediaList ->
            android.util.Log.d("MediaListFragment", "filteredMedia updated: ${mediaList.size} items")
            mediaAdapter.submitList(mediaList)
            binding.tvEmptyState.visibility = if (mediaList.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.storageInfo.observe(viewLifecycleOwner) { storage ->
            storage?.let {
                binding.tvStorageInfo.text = "Storage: ${it.formattedUsed} / ${it.formattedLimit} (${String.format("%.1f", it.calculatedPercentage)}%)"
                binding.tvStorageInfo.visibility = View.VISIBLE
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            android.util.Log.d("MediaListFragment", "isLoading: $isLoading")
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.util.Log.e("MediaListFragment", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
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
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Media"))
    }
    
    private fun handleFileSelection(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val mimeType = contentResolver.getType(uri)
            
            if (mimeType == null || (!mimeType.startsWith("image/") && !mimeType.startsWith("video/"))) {
                Toast.makeText(requireContext(), "Please select an image or video file", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get file extension from MIME type
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/webm" -> "webm"
                "video/quicktime" -> "mov"
                "video/x-msvideo" -> "avi"
                else -> mimeType.substringAfter("/")
            }
            
            // Copy file to cache directory with proper extension
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "upload_${System.currentTimeMillis()}.$extension"
            val tempFile = File(requireContext().cacheDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Upload file
            viewModel.uploadMedia(tempFile)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showMediaOptionsDialog(media: Media) {
        val options = arrayOf("View", "Details", "Delete")
        
        AlertDialog.Builder(requireContext())
            .setTitle(media.originalName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // View
                        val fragment = MediaPreviewFragment.newInstance(media)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    1 -> {
                        // Details
                        val fragment = MediaDetailsFragment.newInstance(media)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    2 -> {
                        // Delete
                        showDeleteConfirmationDialog(media)
                    }
                }
            }
            .show()
    }
    
    private fun showDeleteConfirmationDialog(media: Media) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Media")
            .setMessage("Are you sure you want to delete \"${media.originalName}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMedia(media.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
