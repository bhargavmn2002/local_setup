package com.signox.dashboard.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signox.dashboard.R
import com.signox.dashboard.data.model.UserManagement
import com.signox.dashboard.databinding.FragmentUserListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserListFragment : Fragment() {
    
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter
    
    private var currentFilter: String? = null
    private var currentSearchQuery: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user role from MainActivity
        val sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("user_role", null)
        viewModel.setCurrentUserRole(userRole ?: "")
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupFab()
        setupObservers()
        
        // Load appropriate data based on role
        when (userRole) {
            "SUPER_ADMIN" -> viewModel.loadClientAdmins()
            "CLIENT_ADMIN", "USER_ADMIN" -> viewModel.loadUsers()
            else -> viewModel.loadUsers()
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onUserClick = { user ->
                navigateToUserDetails(user)
            },
            onMoreClick = { user, view ->
                showUserOptionsMenu(user, view)
            }
        )
        
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            currentSearchQuery = text.toString()
            applyFilters()
        }
    }
    
    private fun setupFilters() {
        binding.chipGroupRole.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipClientAdmin) -> "CLIENT_ADMIN"
                checkedIds.contains(R.id.chipUserAdmin) -> "USER_ADMIN"
                checkedIds.contains(R.id.chipStaff) -> "STAFF"
                else -> null
            }
            applyFilters()
        }
    }
    
    private fun setupFab() {
        binding.fabAddUser.setOnClickListener {
            navigateToCreateUser()
        }
    }
    
    private fun setupObservers() {
        val sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("user_role", null)
        
        binding.swipeRefresh.setOnRefreshListener {
            when (userRole) {
                "SUPER_ADMIN" -> viewModel.loadClientAdmins()
                else -> viewModel.loadUsers()
            }
        }
        
        // Observe appropriate data based on role
        if (userRole == "SUPER_ADMIN") {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.clientAdmins.collect { clientAdmins ->
                    // Convert ClientAdmin to UserManagement for display
                    val users = clientAdmins.map { client ->
                        UserManagement(
                            id = client.id,
                            email = client.email,
                            role = com.signox.dashboard.data.model.UserRole.CLIENT_ADMIN,
                            staffRole = null,
                            isActive = client.isActive,
                            createdAt = client.createdAt,
                            clientProfile = client.clientProfile
                        )
                    }
                    applyFiltersToList(users)
                }
            }
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.users.collect { users ->
                    applyFiltersToList(users)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
                binding.progressBar.visibility = if (isLoading && userAdapter.itemCount == 0) {
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
                    
                    // Reload data after successful operation
                    when (userRole) {
                        "SUPER_ADMIN" -> viewModel.loadClientAdmins()
                        else -> viewModel.loadUsers()
                    }
                }
            }
        }
    }
    
    private fun applyFiltersToList(users: List<UserManagement>) {
        var filteredUsers = users
        
        // Apply role filter
        if (currentFilter != null) {
            filteredUsers = filteredUsers.filter { it.role.name == currentFilter }
        }
        
        // Apply search filter
        if (currentSearchQuery.isNotBlank()) {
            filteredUsers = filteredUsers.filter {
                it.email.contains(currentSearchQuery, ignoreCase = true) ||
                it.role.name.contains(currentSearchQuery, ignoreCase = true) ||
                (it.clientProfile?.companyName?.contains(currentSearchQuery, ignoreCase = true) == true)
            }
        }
        
        userAdapter.submitList(filteredUsers)
        binding.tvEmpty.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
        binding.rvUsers.visibility = if (filteredUsers.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun applyFilters() {
        val sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("user_role", null)
        
        val users = if (userRole == "SUPER_ADMIN") {
            // Convert clientAdmins to UserManagement
            viewModel.clientAdmins.value.map { client ->
                UserManagement(
                    id = client.id,
                    email = client.email,
                    role = com.signox.dashboard.data.model.UserRole.CLIENT_ADMIN,
                    staffRole = null,
                    isActive = client.isActive,
                    createdAt = client.createdAt,
                    clientProfile = client.clientProfile
                )
            }
        } else {
            viewModel.users.value
        }
        
        applyFiltersToList(users)
    }
    
    private fun showUserOptionsMenu(user: UserManagement, view: View) {
        val sharedPrefs = requireActivity().getSharedPreferences("signox_prefs", android.content.Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("user_role", null)
        
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.menu_user_options, menu)
            
            // Configure menu based on current user role
            when (userRole) {
                "SUPER_ADMIN" -> {
                    // SUPER_ADMIN managing CLIENT_ADMIN
                    menu.findItem(R.id.action_view_details)?.isVisible = true
                    menu.findItem(R.id.action_edit)?.isVisible = true
                    menu.findItem(R.id.action_reset_password)?.isVisible = false // Don't show reset password
                    menu.findItem(R.id.action_toggle_status)?.isVisible = true
                    menu.findItem(R.id.action_toggle_status)?.title = if (user.isActive) "Suspend" else "Activate"
                    menu.findItem(R.id.action_delete)?.isVisible = true
                }
                "CLIENT_ADMIN" -> {
                    // CLIENT_ADMIN managing USER_ADMIN
                    menu.findItem(R.id.action_view_details)?.isVisible = true
                    menu.findItem(R.id.action_edit)?.isVisible = false
                    menu.findItem(R.id.action_reset_password)?.isVisible = true
                    menu.findItem(R.id.action_toggle_status)?.isVisible = false
                    menu.findItem(R.id.action_delete)?.isVisible = true
                }
                "USER_ADMIN" -> {
                    // USER_ADMIN managing STAFF
                    menu.findItem(R.id.action_view_details)?.isVisible = true
                    menu.findItem(R.id.action_edit)?.isVisible = false
                    menu.findItem(R.id.action_reset_password)?.isVisible = true
                    menu.findItem(R.id.action_toggle_status)?.isVisible = false
                    menu.findItem(R.id.action_delete)?.isVisible = true
                }
                else -> {
                    // Hide all options for unknown roles
                    menu.findItem(R.id.action_view_details)?.isVisible = false
                    menu.findItem(R.id.action_edit)?.isVisible = false
                    menu.findItem(R.id.action_reset_password)?.isVisible = false
                    menu.findItem(R.id.action_toggle_status)?.isVisible = false
                    menu.findItem(R.id.action_delete)?.isVisible = false
                }
            }
            
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_view_details -> {
                        navigateToUserDetails(user)
                        true
                    }
                    R.id.action_edit -> {
                        if (userRole == "SUPER_ADMIN") {
                            // For SUPER_ADMIN, navigate to edit client admin
                            navigateToEditClientAdmin(user)
                        } else {
                            navigateToEditUser(user)
                        }
                        true
                    }
                    R.id.action_reset_password -> {
                        showResetPasswordDialog(user)
                        true
                    }
                    R.id.action_toggle_status -> {
                        if (userRole == "SUPER_ADMIN") {
                            toggleClientAdminStatus(user)
                        } else {
                            toggleUserStatus(user)
                        }
                        true
                    }
                    R.id.action_delete -> {
                        if (userRole == "SUPER_ADMIN") {
                            showDeleteClientAdminConfirmation(user)
                        } else {
                            showDeleteConfirmation(user)
                        }
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }
    
    private fun showResetPasswordDialog(user: UserManagement) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                       android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Enter new password for ${user.email}")
            .setView(input)
            .setPositiveButton("Reset") { _, _ ->
                val newPassword = input.text.toString()
                if (newPassword.isNotBlank()) {
                    viewModel.resetPassword(user.id, newPassword)
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleUserStatus(user: UserManagement) {
        // Toggle active status
        val newStatus = !user.isActive
        viewModel.updateUser(user.id, com.signox.dashboard.data.model.UpdateUserRequest(
            isActive = newStatus
        ))
    }
    
    private fun toggleClientAdminStatus(user: UserManagement) {
        // For SUPER_ADMIN toggling CLIENT_ADMIN status
        viewModel.toggleClientAdminStatus(user.id)
    }
    
    private fun showDeleteConfirmation(user: UserManagement) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.email}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteUser(user.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteClientAdminConfirmation(user: UserManagement) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Client Admin")
            .setMessage("Are you sure you want to delete ${user.email}?\n\nThis will also delete:\n• All User Admins under this client\n• All Staff users\n• All displays, media, and playlists\n• All associated data\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteClientAdmin(user.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToEditClientAdmin(user: UserManagement) {
        // Convert UserManagement to ClientAdmin for editing
        val clientAdmin = com.signox.dashboard.data.model.ClientAdmin(
            id = user.id,
            email = user.email,
            isActive = user.isActive,
            createdAt = user.createdAt,
            clientProfile = user.clientProfile
        )
        viewModel.selectClientAdmin(clientAdmin)
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ClientAdminEditFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToUserDetails(user: UserManagement) {
        viewModel.selectUser(user)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UserDetailsFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToEditUser(user: UserManagement) {
        viewModel.selectUser(user)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UserEditFragment())
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToCreateUser() {
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
