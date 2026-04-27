package com.signox.dashboard.ui.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.data.model.UpdateLayoutRequest
import com.signox.dashboard.databinding.FragmentLayoutEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayoutEditorFragment : Fragment() {
    
    private var _binding: FragmentLayoutEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LayoutViewModel by viewModels()
    private lateinit var sectionAdapter: SectionAdapter
    
    private val layoutId: String by lazy {
        arguments?.getString(ARG_LAYOUT_ID) ?: ""
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        
        // Load layout
        viewModel.loadLayout(layoutId)
        viewModel.loadMedia()
    }
    
    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter(
            onSectionClick = { section ->
                // Navigate to section editor
                val fragment = SectionEditorFragment.newInstance(layoutId, section.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        
        binding.sectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sectionAdapter
        }
    }
    
    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveLayout()
        }
        
        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun saveLayout() {
        val name = binding.layoutNameEditText.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a layout name", Toast.LENGTH_SHORT).show()
            return
        }
        
        val request = UpdateLayoutRequest(
            name = name
        )
        
        viewModel.updateLayout(layoutId, request)
    }
    
    private fun observeViewModel() {
        viewModel.currentLayout.observe(viewLifecycleOwner) { layout ->
            layout?.let {
                binding.layoutNameEditText.setText(it.name)
                binding.layoutDimensions.text = "${it.width}x${it.height} • ${it.orientation}"
                
                // Display sections
                it.sections?.let { sections ->
                    sectionAdapter.submitList(sections)
                    binding.emptySections.visibility = if (sections.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !isLoading
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
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearCurrentLayout()
        _binding = null
    }
    
    companion object {
        private const val ARG_LAYOUT_ID = "layout_id"
        
        fun newInstance(layoutId: String) = LayoutEditorFragment().apply {
            arguments = bundleOf(ARG_LAYOUT_ID to layoutId)
        }
    }
}
