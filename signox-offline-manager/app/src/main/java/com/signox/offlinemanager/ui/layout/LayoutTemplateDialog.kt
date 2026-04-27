package com.signox.offlinemanager.ui.layout

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.offlinemanager.databinding.DialogLayoutTemplateBinding

class LayoutTemplateDialog(
    private val onTemplateSelected: (LayoutTemplate) -> Unit
) : DialogFragment() {
    
    private var _binding: DialogLayoutTemplateBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLayoutTemplateBinding.inflate(layoutInflater)
        
        setupRecyclerView()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Layout Template")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupRecyclerView() {
        val adapter = LayoutTemplateAdapter { template ->
            onTemplateSelected(template)
            dismiss()
        }
        
        binding.recyclerViewTemplates.apply {
            layoutManager = LinearLayoutManager(requireContext()) // Changed from GridLayoutManager to LinearLayoutManager
            this.adapter = adapter
        }
        
        adapter.submitList(LayoutTemplates.templates)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}