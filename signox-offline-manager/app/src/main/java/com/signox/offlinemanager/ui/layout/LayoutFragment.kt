package com.signox.offlinemanager.ui.layout

import android.content.Intent
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
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.databinding.FragmentLayoutBinding

class LayoutFragment : Fragment() {
    
    private var _binding: FragmentLayoutBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LayoutViewModel by viewModels {
        val app = requireActivity().application as SignoXOfflineApplication
        val layoutRepository = LayoutRepository(app.database.layoutDao())
        LayoutViewModelFactory(layoutRepository)
    }
    
    private lateinit var layoutAdapter: LayoutAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutBinding.inflate(inflater, container, false)
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
        layoutAdapter = LayoutAdapter(
            onLayoutClick = { layout -> openLayoutEditor(layout) },
            onEditClick = { layout -> showEditLayoutDialog(layout) },
            onPreviewClick = { layout -> previewLayout(layout) },
            onDeleteClick = { layout -> showDeleteConfirmation(layout) }
        )
        
        binding.recyclerViewLayouts.apply {
            adapter = layoutAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSearchView() {
        binding.searchViewLayouts.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        binding.fabCreateLayout.setOnClickListener {
            showCreateLayoutDialog()
        }
    }
    
    private fun observeViewModel() {
        viewModel.layouts.observe(viewLifecycleOwner) { layouts ->
            val filteredLayouts = filterLayouts(layouts, viewModel.searchQuery.value ?: "")
            layoutAdapter.submitList(filteredLayouts)
            
            binding.layoutEmptyState.visibility = if (layouts.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            val layouts = viewModel.layouts.value ?: emptyList()
            val filteredLayouts = filterLayouts(layouts, query)
            layoutAdapter.submitList(filteredLayouts)
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
    
    private fun filterLayouts(layouts: List<Layout>, query: String): List<Layout> {
        if (query.isEmpty()) return layouts
        
        return layouts.filter { layout ->
            layout.name.contains(query, ignoreCase = true) ||
            layout.description?.contains(query, ignoreCase = true) == true
        }
    }
    
    private fun showCreateLayoutDialog() {
        val dialog = CreateLayoutDialog(
            onCreateLayout = { name, description, width, height, template ->
                viewModel.createLayout(name, description, width, height, template)
            }
        )
        dialog.show(parentFragmentManager, "CreateLayoutDialog")
    }
    
    private fun showEditLayoutDialog(layout: Layout) {
        val dialog = CreateLayoutDialog(
            onCreateLayout = { name, description, width, height, template ->
                // TODO: Implement layout update
                Toast.makeText(requireContext(), "Edit functionality coming soon", Toast.LENGTH_SHORT).show()
            },
            existingLayout = layout
        )
        dialog.show(parentFragmentManager, "EditLayoutDialog")
    }
    
    private fun openLayoutEditor(layout: Layout) {
        val intent = Intent(requireContext(), LayoutEditorActivity::class.java)
        intent.putExtra("layout_id", layout.id)
        startActivity(intent)
    }
    
    private fun previewLayout(layout: Layout) {
        // TODO: Implement layout preview
        Toast.makeText(requireContext(), "Preview functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteConfirmation(layout: Layout) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Layout")
            .setMessage("Are you sure you want to delete \"${layout.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteLayout(layout)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}