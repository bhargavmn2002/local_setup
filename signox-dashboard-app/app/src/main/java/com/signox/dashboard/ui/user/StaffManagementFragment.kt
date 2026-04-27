package com.signox.dashboard.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.dashboard.R
import com.signox.dashboard.data.model.UserManagement
import com.signox.dashboard.databinding.FragmentStaffManagementBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffManagementFragment : Fragment() {
    
    private var _binding: FragmentStaffManagementBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by viewModels()
    private lateinit var staffAdapter: UserAdapter
    
    private var currentFilter: String? = null
    private var currentSearchQuery: String = ""
    private val selectedStaff = mutableSetOf<String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffManagementBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupFab()
        setupObservers()
        
        // Load staff users
        viewModel.loadUsers()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_bulk_delete -> {
                    if (selectedStaff.isNotEmpty()) {
                        showBulkDeleteConfirmation()
                    } else {
                        Toast.makeText(requireContext(), "No staff selected", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        staffAdapter = UserAdapter(
            onUserClick = { user ->
                navigateToStaffDetails(user)
            },
            onMoreClick = { user, view ->
                showStaffOptionsMenu(user, view)
            }
        )
        
        binding.rvStaff.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffAdapter
        }
    }
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            currentSearchQuery = text.toString()
            applyFilters()
        }
    }
    
    private fun setupFilters() {
        binding.chipGroupStaffRole.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipDisplayManager) -> "DISPLAY_MANAGER"
                checkedIds.contains(R.id.chipBroadcastManager) -> "BROADCAST_MANAGER"
                checkedIds.contains(R.id.chipContentManager) -> "CONTENT_MANAGER"
                checkedIds.contains(R.id.chipAnalyst) -> "ANALYST"
                else -> null
            }
            applyFilters()
        }
    }
    
    private fun setupFab() {
        binding.fabAddStaff.setOnClickListener {
            navigateToCreateStaff()
        }
    }
    
    private fun setupObservers() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadUsers()
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collect { users ->
                // Filter only STAFF users
                val staffUsers = users.filter { it.role.name == "STAFF" }
                applyFiltersToList(staffUsers)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
                binding.progressBar.visibility = if (isLoading && staffAdapter.itemCount == 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                    viewModel.loadUsers() // Refresh list
                    selectedStaff.clear()
                    updateSelectionUI()
                }
            }
        }
    }
    
    private fun applyFiltersToList(staffUsers: List<UserManagement>) {
        var filteredStaff = staffUsers
        
        // Apply staff role filter
        if (currentFilter != null) {
            filteredStaff = filteredStaff.filter { it.staffRole?.name == currentFilter }
        }
        
        // Apply search filter
        if (currentSearchQuery.isNotBlank()) {
            filteredStaff = filteredStaff.filter {
                it.email.contains(currentSearchQuery, ignoreCase = true) ||
                (it.staffRole?.name?.contains(currentSearchQuery, ignoreCase = true) == true)
            }
        }
        
        staffAdapter.submitList(filteredStaff)
        binding.tvEmpty.visibility = if (filteredStaff.isEmpty()) View.VISIBLE else View.GONE
        binding.rvStaff.visibility = if (filteredStaff.isEmpty()) View.GONE else View.VISIBLE
        
        // Update stats
        binding.tvTotalStaff.text = "Total Staff: ${staffUsers.size}"
        binding.tvFilteredStaff.text = "Showing: ${filteredStaff.size}"
    }
    
    private fun applyFilters() {
        val staffUsers = viewModel.users.value.filter { it.role.name == "STAFF" }
        applyFiltersToList(staffUsers)
    }
    
    private fun updateSelectionUI() {
        if (selectedStaff.isNotEmpty()) {
            binding.toolbar.title = "${selectedStaff.size} selected"
            binding.toolbar.menu.findItem(R.id.action_bulk_delete)?.isVisible = true
        } else {
            binding.toolbar.title = "Staff Management"
            binding.toolbar.menu.findItem(R.id.action_bulk_delete)?.isVisible = false
        }
    }
    
    private fun showStaffOptionsMenu(staff: UserManagement, view: View) {
        android.widget.PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.menu_staff_options, menu)
            
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_view_details -> {
                        navigateToStaffDetails(staff)
                        true
                    }
                    R.id.action_edit -> {
                        navigateToEditStaff(staff)
                        true
                    }
                    R.id.action_reset_password -> {
                        showResetPasswordDialog(staff)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmation(staff)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }
    
    private fun showResetPasswordDialog(staff: UserManagement) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                       android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Enter new password for ${staff.email}")
            .setView(input)
            .setPositiveButton("Reset") { _, _ ->
                val newPassword = input.text.toString()
                if (newPassword.isNotBlank()) {
                    viewModel.resetPassword(staff.id, newPassword)
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteConfirmation(staff: UserManagement) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Staff")
            .setMessage("Are you sure you want to delete ${staff.email}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteUser(staff.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBulkDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Multiple Staff")
            .setMessage("Are you sure you want to delete ${selectedStaff.size} staff members?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.bulkDeleteUsers(selectedStaff.toList())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToStaffDetails(staff: UserManagement) {
        viewModel.selectUser(staff)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UserDetailsFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToEditStaff(staff: UserManagement) {
        viewModel.selectUser(staff)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UserEditFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToCreateStaff() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UserCreateFragment())
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
