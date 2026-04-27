package com.signox.dashboard.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.databinding.FragmentDisplaySelectionBinding
import com.signox.dashboard.ui.display.DisplayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplaySelectionFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentDisplaySelectionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DisplayViewModel by viewModels()
    private lateinit var adapter: DisplayCheckboxAdapter
    
    private val preSelectedIds: List<String> by lazy {
        arguments?.getStringArrayList(ARG_SELECTED_IDS) ?: emptyList()
    }
    
    private var onDisplaysSelected: ((List<String>) -> Unit)? = null
    private val selectedDisplayIds = mutableListOf<String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplaySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        selectedDisplayIds.addAll(preSelectedIds)
        
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        
        viewModel.loadDisplays()
    }
    
    private fun setupRecyclerView() {
        adapter = DisplayCheckboxAdapter(preSelectedIds) { display, isSelected ->
            if (isSelected) {
                selectedDisplayIds.add(display.id)
            } else {
                selectedDisplayIds.remove(display.id)
            }
            updateSelectedCount()
        }
        
        binding.displaysRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DisplaySelectionFragment.adapter
        }
    }
    
    private fun setupButtons() {
        binding.confirmButton.setOnClickListener {
            if (selectedDisplayIds.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one display", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onDisplaysSelected?.invoke(selectedDisplayIds)
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun observeViewModel() {
        viewModel.displays.observe(viewLifecycleOwner) { displays ->
            adapter.submitList(displays)
            binding.emptyState.visibility = if (displays.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun updateSelectedCount() {
        binding.selectedCount.text = "${selectedDisplayIds.size} selected"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_SELECTED_IDS = "selected_ids"
        
        fun newInstance(
            selectedIds: List<String>,
            onSelected: (List<String>) -> Unit
        ): DisplaySelectionFragment {
            return DisplaySelectionFragment().apply {
                arguments = bundleOf(ARG_SELECTED_IDS to ArrayList(selectedIds))
                this.onDisplaysSelected = onSelected
            }
        }
    }
}
