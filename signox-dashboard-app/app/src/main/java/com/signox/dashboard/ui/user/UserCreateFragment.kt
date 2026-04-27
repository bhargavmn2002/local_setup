package com.signox.dashboard.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.signox.dashboard.R
import com.signox.dashboard.data.model.CreateUserRequest
import com.signox.dashboard.data.model.UserRole
import com.signox.dashboard.databinding.FragmentUserCreateBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserCreateFragment : Fragment() {
    
    private var _binding: FragmentUserCreateBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by activityViewModels()
    private var currentUserRole: UserRole? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCreateBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        
        binding.btnCreate.setOnClickListener {
            createUser()
        }
        
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupUI() {
        // Get current user role from shared preferences or auth context
        val sharedPrefs = requireContext().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val roleString = sharedPrefs.getString("user_role", null)
        currentUserRole = roleString?.let { 
            try {
                UserRole.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        
        when (currentUserRole) {
            UserRole.SUPER_ADMIN -> {
                // Super Admin creates CLIENT_ADMIN
                binding.tvRoleInfo.text = "As a Super Admin, you can create Client Admin accounts with company profiles"
                binding.layoutCompanyFields.visibility = View.VISIBLE
                binding.layoutStaffRole.visibility = View.GONE
            }
            UserRole.CLIENT_ADMIN -> {
                // Client Admin creates USER_ADMIN
                binding.tvRoleInfo.text = "As a Client Admin, you can create User Admin accounts to manage displays and content"
                binding.layoutCompanyFields.visibility = View.GONE
                binding.layoutStaffRole.visibility = View.GONE
            }
            UserRole.USER_ADMIN -> {
                // User Admin creates STAFF
                binding.tvRoleInfo.text = "As a User Admin, you can create Staff accounts with specific roles"
                binding.layoutCompanyFields.visibility = View.GONE
                binding.layoutStaffRole.visibility = View.VISIBLE
                setupStaffRoleSpinner()
            }
            else -> {
                binding.tvRoleInfo.text = "You don't have permission to create users"
                binding.btnCreate.isEnabled = false
            }
        }
    }
    
    private fun setupStaffRoleSpinner() {
        val staffRoles = listOf(
            "Content Manager" to "CONTENT_MANAGER",
            "Display Manager" to "DISPLAY_MANAGER",
            "Proof of Play Manager" to "POP_MANAGER",
            "Viewer" to "CMS_VIEWER"
        )
        
        val displayNames = staffRoles.map { it.first }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            displayNames
        )
        
        binding.spinnerStaffRole.setAdapter(adapter)
        binding.spinnerStaffRole.setText(displayNames[0], false)
        
        // Store the mapping for later use
        binding.spinnerStaffRole.tag = staffRoles
    }
    
    private fun createUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(requireContext(), "Email and password are required", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        val request = when (currentUserRole) {
            UserRole.SUPER_ADMIN -> {
                // Creating CLIENT_ADMIN
                val companyName = binding.etCompanyName.text.toString().trim()
                if (companyName.isBlank()) {
                    Toast.makeText(requireContext(), "Company name is required", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val maxDisplays = binding.etMaxDisplays.text.toString().toIntOrNull() ?: 10
                val maxUsers = binding.etMaxUsers.text.toString().toIntOrNull() ?: 5
                
                CreateUserRequest(
                    email = email,
                    password = password,
                    companyName = companyName,
                    maxDisplays = maxDisplays,
                    maxUsers = maxUsers
                )
            }
            UserRole.USER_ADMIN -> {
                // Creating STAFF
                val selectedDisplayName = binding.spinnerStaffRole.text.toString()
                @Suppress("UNCHECKED_CAST")
                val staffRoles = binding.spinnerStaffRole.tag as? List<Pair<String, String>>
                val staffRole = staffRoles?.find { it.first == selectedDisplayName }?.second ?: "CONTENT_MANAGER"
                
                CreateUserRequest(
                    email = email,
                    password = password,
                    staffRole = staffRole
                )
            }
            else -> {
                // CLIENT_ADMIN creating USER_ADMIN (no extra fields needed)
                CreateUserRequest(
                    email = email,
                    password = password
                )
            }
        }
        
        viewModel.createUser(request)
        
        // Observe success and navigate back
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                if (message != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
