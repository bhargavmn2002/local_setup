package com.signox.dashboard.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.databinding.FragmentUserEditBinding
import com.signox.dashboard.data.model.UpdateUserRequest
import com.signox.dashboard.data.model.StaffRole
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserEditFragment : Fragment() {
    
    private var _binding: FragmentUserEditBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by viewModels()
    private var selectedStaffRole: StaffRole? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserEditBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupObservers()
        loadUserData()
        setupListeners()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedUser.collect { user ->
                user?.let {
                    binding.etEmail.setText(it.email)
                    
                    // Show/hide staff role based on user type
                    if (it.role.name == "STAFF" && it.staffRole != null) {
                        binding.layoutStaffRole.visibility = View.VISIBLE
                        selectedStaffRole = it.staffRole
                        setupStaffRoleSpinner()
                    } else {
                        binding.layoutStaffRole.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun setupStaffRoleSpinner() {
        val staffRoles = StaffRole.values().map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            staffRoles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStaffRole.adapter = adapter
        
        // Set current selection
        selectedStaffRole?.let { role ->
            val position = staffRoles.indexOf(role.name)
            if (position >= 0) {
                binding.spinnerStaffRole.setSelection(position)
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveChanges()
        }
        
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun saveChanges() {
        val email = binding.etEmail.text.toString().trim()
        
        // Validation
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return
        }
        
        // Get staff role if applicable
        val staffRoleName = if (binding.layoutStaffRole.visibility == View.VISIBLE) {
            binding.spinnerStaffRole.selectedItem?.toString()
        } else null
        
        val staffRole = staffRoleName?.let {
            try {
                StaffRole.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        
        val request = UpdateUserRequest(
            email = email,
            staffRole = staffRole?.name
        )
        
        viewModel.selectedUser.value?.let { user ->
            viewModel.updateUser(user.id, request)
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
