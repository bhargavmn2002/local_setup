package com.signox.dashboard.ui.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.data.model.CreateSectionItemRequest
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.data.model.UpdateSectionRequest
import com.signox.dashboard.databinding.FragmentSectionEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SectionEditorFragment : Fragment() {
    
    private var _binding: FragmentSectionEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LayoutViewModel by viewModels()
    private lateinit var itemAdapter: SectionItemAdapter
    
    private val layoutId: String by lazy {
        arguments?.getString(ARG_LAYOUT_ID) ?: ""
    }
    
    private val sectionId: String by lazy {
        arguments?.getString(ARG_SECTION_ID) ?: ""
    }
    
    private val currentItems = mutableListOf<CreateSectionItemRequest>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSectionEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        
        // Load layout and media
        viewModel.loadLayout(layoutId)
        viewModel.loadMedia()
    }
    
    private fun setupRecyclerView() {
        itemAdapter = SectionItemAdapter(
            onRemoveClick = { position ->
                currentItems.removeAt(position)
                updateItemsList()
            },
            onDurationChange = { position, duration ->
                currentItems[position] = currentItems[position].copy(duration = duration)
            }
        )
        
        binding.itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemAdapter
        }
    }
    
    private fun setupButtons() {
        binding.addMediaButton.setOnClickListener {
            showMediaSelectionDialog()
        }
        
        binding.saveButton.setOnClickListener {
            saveSection()
        }
        
        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun showMediaSelectionDialog() {
        val mediaList = viewModel.mediaList.value ?: emptyList()
        
        android.util.Log.d("SectionEditor", "Media list size: ${mediaList.size}")
        
        if (mediaList.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Media Available")
                .setMessage("Please upload some media files first from the Media section.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val mediaNames = mediaList.map { "${it.name} (${it.type})" }.toTypedArray()
        val selectedItems = BooleanArray(mediaList.size)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Media")
            .setMultiChoiceItems(mediaNames, selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("Add") { _, _ ->
                var addedCount = 0
                selectedItems.forEachIndexed { index, isSelected ->
                    if (isSelected) {
                        val media = mediaList[index]
                        currentItems.add(
                            CreateSectionItemRequest(
                                mediaId = media.id,
                                order = currentItems.size,
                                duration = media.duration ?: 10,
                                resizeMode = "FIT",
                                rotation = 0
                            )
                        )
                        addedCount++
                    }
                }
                if (addedCount > 0) {
                    Toast.makeText(requireContext(), "$addedCount media item(s) added", Toast.LENGTH_SHORT).show()
                }
                updateItemsList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateItemsList() {
        // Reorder items
        currentItems.forEachIndexed { index, item ->
            currentItems[index] = item.copy(order = index)
        }
        
        itemAdapter.submitList(currentItems.toList())
        binding.emptyItems.visibility = if (currentItems.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun saveSection() {
        val request = UpdateSectionRequest(
            items = currentItems
        )
        
        viewModel.updateSection(layoutId, sectionId, request)
    }
    
    private fun observeViewModel() {
        viewModel.currentLayout.observe(viewLifecycleOwner) { layout ->
            layout?.let {
                val section = it.sections?.find { s -> s.id == sectionId }
                section?.let { sec ->
                    binding.sectionName.text = sec.name
                    binding.sectionInfo.text = "${sec.width.toInt()}x${sec.height.toInt()}% at (${sec.x.toInt()}%, ${sec.y.toInt()}%)"
                    
                    // Load existing items
                    if (currentItems.isEmpty()) {
                        sec.items?.forEach { item ->
                            currentItems.add(
                                CreateSectionItemRequest(
                                    mediaId = item.mediaId,
                                    order = item.order,
                                    duration = item.duration,
                                    orientation = item.orientation,
                                    resizeMode = item.resizeMode,
                                    rotation = item.rotation
                                )
                            )
                        }
                        updateItemsList()
                    }
                }
            }
        }
        
        viewModel.mediaList.observe(viewLifecycleOwner) { mediaList ->
            android.util.Log.d("SectionEditor", "Media list updated: ${mediaList.size} items")
            if (mediaList.isEmpty()) {
                android.util.Log.w("SectionEditor", "Media list is empty!")
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.util.Log.e("SectionEditor", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_LAYOUT_ID = "layout_id"
        private const val ARG_SECTION_ID = "section_id"
        
        fun newInstance(layoutId: String, sectionId: String) = SectionEditorFragment().apply {
            arguments = bundleOf(
                ARG_LAYOUT_ID to layoutId,
                ARG_SECTION_ID to sectionId
            )
        }
    }
}
