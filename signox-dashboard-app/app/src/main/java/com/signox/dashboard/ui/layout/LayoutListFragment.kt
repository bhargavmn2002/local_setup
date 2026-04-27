package com.signox.dashboard.ui.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.signox.dashboard.R
import com.signox.dashboard.databinding.FragmentLayoutListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayoutListFragment : Fragment() {
    
    private var _binding: FragmentLayoutListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LayoutViewModel by viewModels()
    private lateinit var adapter: LayoutAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        setupFab()
        setupSwipeRefresh()
        observeViewModel()
        
        // Load layouts
        viewModel.loadLayouts()
    }
    
    private fun setupRecyclerView() {
        adapter = LayoutAdapter(
            onLayoutClick = { layout ->
                // Navigate to layout editor
                val fragment = LayoutEditorFragment.newInstance(layout.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { layout ->
                showDeleteConfirmation(layout.id, layout.name)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LayoutListFragment.adapter
        }
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }
    
    private fun setupFab() {
        binding.fabCreateLayout.setOnClickListener {
            // Navigate to template selection
            val fragment = LayoutTemplatesFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadLayouts()
        }
    }
    
    private fun observeViewModel() {
        viewModel.layouts.observe(viewLifecycleOwner) { layouts ->
            adapter.submitList(layouts)
            binding.emptyView.visibility = if (layouts.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (layouts.isEmpty()) View.GONE else View.VISIBLE
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }
    
    private fun showDeleteConfirmation(layoutId: String, layoutName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Layout")
            .setMessage("Are you sure you want to delete \"$layoutName\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteLayout(layoutId) {
                    viewModel.loadLayouts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = LayoutListFragment()
    }
}
