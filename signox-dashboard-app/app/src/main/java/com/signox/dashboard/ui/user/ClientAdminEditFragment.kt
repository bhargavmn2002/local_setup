package com.signox.dashboard.ui.user

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.FragmentClientAdminEditBinding
import com.signox.dashboard.data.model.UpdateClientAdminRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ClientAdminEditFragment : Fragment() {
    
    private var _binding: FragmentClientAdminEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by viewModels()
    private var selectedLicenseExpiry: Date? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientAdminEditBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
        loadClientAdminData()
        setupListeners()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun loadClientAdminData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedClientAdmin.collect { clientAdmin ->
                clientAdmin?.let {
                    binding.etCompanyName.setText(it.clientProfile?.companyName ?: "")
                    binding.etMaxDisplays.setText(it.clientProfile?.maxDisplays?.toString() ?: "10")
                    binding.etMaxUsers.setText(it.clientProfile?.maxUsers?.toString() ?: "5")
                    binding.etMaxStorageMB.setText(it.clientProfile?.maxStorageMB?.toString() ?: "25")
                    binding.etContactEmail.setText(it.clientProfile?.contactEmail ?: it.email)
                    binding.etContactPhone.setText(it.clientProfile?.contactPhone ?: "")
                    
                    // Set license expiry if exists
                    it.clientProfile?.licenseExpiry?.let { expiry ->
                        try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            selectedLicenseExpiry = dateFormat.parse(expiry)
                            updateLicenseExpiryDisplay()
                        } catch (e: Exception) {
                            // Ignore parse errors
                        }
                    }
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnLicenseExpiry.setOnClickListener {
            showDatePicker()
        }
        
        binding.btnClearLicenseExpiry.setOnClickListener {
            selectedLicenseExpiry = null
            updateLicenseExpiryDisplay()
        }
        
        binding.btnSave.setOnClickListener {
            saveChanges()
        }
        
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedLicenseExpiry?.let { calendar.time = it }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedLicenseExpiry = calendar.time
                updateLicenseExpiryDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun updateLicenseExpiryDisplay() {
        if (selectedLicenseExpiry != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.btnLicenseExpiry.text = dateFormat.format(selectedLicenseExpiry!!)
            binding.btnClearLicenseExpiry.visibility = View.VISIBLE
        } else {
            binding.btnLicenseExpiry.text = "Select License Expiry (Optional)"
            binding.btnClearLicenseExpiry.visibility = View.GONE
        }
    }
    
    private fun saveChanges() {
        val companyName = binding.etCompanyName.text.toString().trim()
        val maxDisplaysStr = binding.etMaxDisplays.text.toString().trim()
        val maxUsersStr = binding.etMaxUsers.text.toString().trim()
        val maxStorageMBStr = binding.etMaxStorageMB.text.toString().trim()
        val contactEmail = binding.etContactEmail.text.toString().trim()
        val contactPhone = binding.etContactPhone.text.toString().trim()
        
        // Validation
        if (companyName.isEmpty()) {
            binding.etCompanyName.error = "Company name is required"
            return
        }
        
        val maxDisplays = maxDisplaysStr.toIntOrNull()
        if (maxDisplays == null || maxDisplays < 0) {
            binding.etMaxDisplays.error = "Invalid number"
            return
        }
        
        val maxUsers = maxUsersStr.toIntOrNull()
        if (maxUsers == null || maxUsers < 0) {
            binding.etMaxUsers.error = "Invalid number"
            return
        }
        
        val maxStorageMB = maxStorageMBStr.toIntOrNull()
        if (maxStorageMB == null || maxStorageMB < 1) {
            binding.etMaxStorageMB.error = "Invalid number"
            return
        }
        
        // Format license expiry for API
        val licenseExpiryStr = selectedLicenseExpiry?.let {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.format(it)
        }
        
        val request = UpdateClientAdminRequest(
            companyName = companyName,
            maxDisplays = maxDisplays,
            maxUsers = maxUsers,
            maxStorageMB = maxStorageMB,
            licenseExpiry = licenseExpiryStr,
            contactEmail = contactEmail.ifEmpty { null },
            contactPhone = contactPhone.ifEmpty { null }
        )
        
        viewModel.selectedClientAdmin.value?.let { clientAdmin ->
            viewModel.updateClientAdmin(clientAdmin.id, request)
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
                binding.btnCancel.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
