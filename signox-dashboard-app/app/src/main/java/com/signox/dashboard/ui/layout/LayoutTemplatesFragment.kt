package com.signox.dashboard.ui.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.signox.dashboard.R
import com.signox.dashboard.data.model.CreateLayoutRequest
import com.signox.dashboard.data.model.CreateSectionRequest
import com.signox.dashboard.databinding.FragmentLayoutTemplatesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LayoutTemplatesFragment : Fragment() {
    
    private var _binding: FragmentLayoutTemplatesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LayoutViewModel by viewModels()
    private lateinit var adapter: TemplateAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLayoutTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = TemplateAdapter { template ->
            // Show dialog to enter layout name
            val builder = android.app.AlertDialog.Builder(requireContext())
            val input = android.widget.EditText(requireContext())
            input.hint = "Layout name"
            input.setPadding(50, 30, 50, 30)
            
            builder.setTitle("Create Layout")
                .setMessage("Enter a name for your layout")
                .setView(input)
                .setPositiveButton("Create") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        createLayoutFromTemplate(template, name)
                    } else {
                        Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@LayoutTemplatesFragment.adapter
        }
        
        // Load templates
        adapter.submitList(LayoutTemplates.getAllTemplates())
    }
    
    private fun createLayoutFromTemplate(template: com.signox.dashboard.data.model.LayoutTemplate, name: String) {
        val sections = template.sections.map { section ->
            CreateSectionRequest(
                name = section.name,
                order = section.order,
                x = section.x,
                y = section.y,
                width = section.width,
                height = section.height,
                loopEnabled = true
            )
        }
        
        val request = CreateLayoutRequest(
            name = name,
            description = template.description,
            width = template.width,
            height = template.height,
            orientation = template.orientation,
            sections = sections
        )
        
        viewModel.createLayout(request) { layout ->
            // Navigate to editor
            val fragment = LayoutEditorFragment.newInstance(layout.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
    
    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
