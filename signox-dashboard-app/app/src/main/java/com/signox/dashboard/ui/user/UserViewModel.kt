package com.signox.dashboard.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.model.*
import com.signox.dashboard.data.repository.UserRepository
import com.signox.dashboard.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _users = MutableStateFlow<List<UserManagement>>(emptyList())
    val users: StateFlow<List<UserManagement>> = _users.asStateFlow()
    
    private val _clientAdmins = MutableStateFlow<List<ClientAdmin>>(emptyList())
    val clientAdmins: StateFlow<List<ClientAdmin>> = _clientAdmins.asStateFlow()
    
    private val _selectedUser = MutableStateFlow<UserManagement?>(null)
    val selectedUser: StateFlow<UserManagement?> = _selectedUser.asStateFlow()
    
    private val _selectedClientAdmin = MutableStateFlow<ClientAdmin?>(null)
    val selectedClientAdmin: StateFlow<ClientAdmin?> = _selectedClientAdmin.asStateFlow()
    
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadCurrentUserRole()
    }
    
    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            // Get current user role from auth repository or shared preferences
            // For now, we'll load it when needed
        }
    }
    
    fun setCurrentUserRole(role: String) {
        _currentUserRole.value = role
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.getUsers()) {
                is NetworkResult.Success -> {
                    _users.value = result.data ?: emptyList()
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun loadClientAdmins() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.getClientAdmins()) {
                is NetworkResult.Success -> {
                    _clientAdmins.value = result.data ?: emptyList()
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun createUser(request: CreateUserRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.createUser(request)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "User created successfully"
                    _isLoading.value = false
                    loadUsers() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun createClientAdmin(request: CreateClientAdminRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.createClientAdmin(request)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "Client admin created successfully"
                    _isLoading.value = false
                    loadClientAdmins() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun updateUser(userId: String, request: UpdateUserRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.updateUser(userId, request)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "User updated successfully"
                    _isLoading.value = false
                    loadUsers() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun updateClientAdmin(clientAdminId: String, request: UpdateClientAdminRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.updateClientAdmin(clientAdminId, request)) {
                is NetworkResult.Success -> {
                    _successMessage.value = "Client admin updated successfully"
                    _isLoading.value = false
                    loadClientAdmins() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun toggleClientAdminStatus(clientAdminId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.toggleClientAdminStatus(clientAdminId)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "Status updated successfully"
                    _isLoading.value = false
                    loadClientAdmins() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.deleteUser(userId)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "User deleted successfully"
                    _isLoading.value = false
                    loadUsers() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun deleteClientAdmin(clientAdminId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.deleteClientAdmin(clientAdminId)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "Client admin deleted successfully"
                    _isLoading.value = false
                    loadClientAdmins() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun bulkDeleteUsers(userIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.bulkDeleteUsers(userIds)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "Users deleted successfully"
                    _isLoading.value = false
                    loadUsers() // Refresh list
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun resetPassword(userId: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.resetPassword(userId, newPassword)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "Password reset successfully"
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }
    
    fun selectUser(user: UserManagement?) {
        _selectedUser.value = user
    }
    
    fun selectClientAdmin(clientAdmin: ClientAdmin?) {
        _selectedClientAdmin.value = clientAdmin
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun filterUsers(query: String): List<UserManagement> {
        if (query.isBlank()) return _users.value
        return _users.value.filter {
            it.email.contains(query, ignoreCase = true) ||
            it.role.name.contains(query, ignoreCase = true)
        }
    }
    
    fun filterByRole(role: String?): List<UserManagement> {
        if (role.isNullOrBlank() || role == "ALL") return _users.value
        return _users.value.filter { it.role.name == role }
    }
}
