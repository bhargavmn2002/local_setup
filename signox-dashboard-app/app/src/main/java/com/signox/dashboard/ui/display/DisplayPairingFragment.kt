package com.signox.dashboard.ui.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signox.dashboard.databinding.FragmentDisplayPairingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayPairingFragment : Fragment() {
    
    private var _binding: FragmentDisplayPairingBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DisplayViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayPairingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupButtons() {
        // Pair display
        binding.btnPairDisplay.setOnClickListener {
            val pairingCode = binding.etPairingCode.text?.toString()?.trim()
            val displayName = binding.etDisplayName.text?.toString()?.trim()
            
            // Validate pairing code (must be 6 digits)
            if (pairingCode.isNullOrEmpty() || !pairingCode.matches(Regex("^\\d{6}$"))) {
                Toast.makeText(requireContext(), "Pairing code must be exactly 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (displayName.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter a display name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.pairDisplay(pairingCode, displayName)
        }
    }
    
    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPairDisplay.isEnabled = !isLoading
        }
        
        // Error messages
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Success messages
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
                
                // Navigate back to display list
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
