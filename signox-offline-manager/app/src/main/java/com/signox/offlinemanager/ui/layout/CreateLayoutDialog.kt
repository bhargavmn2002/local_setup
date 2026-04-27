package com.signox.offlinemanager.ui.layout

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.offlinemanager.R
import com.signox.offlinemanager.databinding.DialogCreateLayoutBinding

class CreateLayoutDialog(
    private val onCreateLayout: (String, String, Int, Int, LayoutTemplate?) -> Unit,
    private val existingLayout: com.signox.offlinemanager.data.model.Layout? = null
) : DialogFragment() {
    
    private var _binding: DialogCreateLayoutBinding? = null
    private val binding get() = _binding!!
    
    private var selectedTemplate: LayoutTemplate? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateLayoutBinding.inflate(layoutInflater)
        
        setupViews()
        
        val title = if (existingLayout != null) "Edit Layout" else "Create New Layout"
        val positiveButtonText = if (existingLayout != null) "Update" else "Create"
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                createLayout()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupViews() {
        // Setup resolution spinner
        val resolutions = arrayOf(
            "1920 × 1080 (Full HD)",
            "1366 × 768 (HD)",
            "1280 × 720 (HD)",
            "3840 × 2160 (4K)",
            "2560 × 1440 (QHD)",
            "Custom"
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resolutions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerResolution.adapter = adapter
        
        // Handle custom resolution visibility
        binding.spinnerResolution.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val isCustom = position == resolutions.size - 1
                    binding.layoutCustomResolution.visibility = if (isCustom) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                }
                
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        )
        
        // Template selection button
        binding.buttonSelectTemplate.setOnClickListener {
            showTemplateSelector()
        }
        
        // Pre-fill if editing
        existingLayout?.let { layout ->
            binding.editTextLayoutName.setText(layout.name)
            binding.editTextLayoutDescription.setText(layout.description)
            
            // Find matching resolution or set to custom
            val resolutionText = "${layout.width} × ${layout.height}"
            val matchingIndex = resolutions.indexOfFirst { it.contains(resolutionText) }
            
            if (matchingIndex != -1) {
                binding.spinnerResolution.setSelection(matchingIndex)
            } else {
                binding.spinnerResolution.setSelection(resolutions.size - 1) // Custom
                binding.editTextWidth.setText(layout.width.toString())
                binding.editTextHeight.setText(layout.height.toString())
            }
        }
        
        updateTemplateDisplay()
    }
    
    private fun showTemplateSelector() {
        val templateDialog = LayoutTemplateDialog { template ->
            selectedTemplate = template
            updateTemplateDisplay()
        }
        templateDialog.show(parentFragmentManager, "TemplateSelector")
    }
    
    private fun updateTemplateDisplay() {
        if (selectedTemplate != null) {
            binding.textSelectedTemplate.text = "Template: ${selectedTemplate!!.name}"
            binding.textSelectedTemplate.visibility = android.view.View.VISIBLE
        } else {
            binding.textSelectedTemplate.text = "No template selected (blank layout)"
            binding.textSelectedTemplate.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun createLayout() {
        val name = binding.editTextLayoutName.text.toString().trim()
        val description = binding.editTextLayoutDescription.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.editTextLayoutName.error = "Layout name is required"
            return
        }
        
        val (width, height) = getSelectedResolution()
        
        if (width <= 0 || height <= 0) {
            binding.editTextWidth.error = "Invalid resolution"
            binding.editTextHeight.error = "Invalid resolution"
            return
        }
        
        android.util.Log.d("CreateLayoutDialog", "Creating layout: $name, ${width}x${height}, template: ${selectedTemplate?.name}")
        onCreateLayout(name, description, width, height, selectedTemplate)
    }
    
    private fun getSelectedResolution(): Pair<Int, Int> {
        val selectedPosition = binding.spinnerResolution.selectedItemPosition
        
        return when (selectedPosition) {
            0 -> Pair(1920, 1080) // Full HD
            1 -> Pair(1366, 768)  // HD
            2 -> Pair(1280, 720)  // HD
            3 -> Pair(3840, 2160) // 4K
            4 -> Pair(2560, 1440) // QHD
            5 -> { // Custom
                val width = binding.editTextWidth.text.toString().toIntOrNull() ?: 0
                val height = binding.editTextHeight.text.toString().toIntOrNull() ?: 0
                Pair(width, height)
            }
            else -> Pair(1920, 1080)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}