package com.signox.dashboard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.model.*
import com.signox.dashboard.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()
    
    private val _hierarchy = MutableStateFlow<HierarchyInfo?>(null)
    val hierarchy: StateFlow<HierarchyInfo?> = _hierarchy.asStateFlow()
    
    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.getProfile()) {
                is NetworkResult.Success -> {
                    _profile.value = result.data
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
    
    fun loadHierarchy() {
        viewModelScope.launch {
            when (val result = repository.getHierarchy()) {
                is NetworkResult.Success -> {
                    _hierarchy.value = result.data
                }
                is NetworkResult.Error -> {
                    // Don't show error for hierarchy, just log it
                    _hierarchy.value = null
                }
                is NetworkResult.Loading -> {
                    // No loading state needed
                }
            }
        }
    }
    
    fun updateProfile(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val request = UpdateProfileRequest(name = name)
            when (val result = repository.updateProfile(request)) {
                is NetworkResult.Success -> {
                    _profile.value = result.data
                    _successMessage.value = "Profile updated successfully"
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
    
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val request = ChangePasswordRequest(currentPassword, newPassword)
            when (val result = repository.changePassword(request)) {
                is NetworkResult.Success -> {
                    _successMessage.value = result.data ?: "Password changed successfully"
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
    
    fun loadAccountInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.getAccountInfo()) {
                is NetworkResult.Success -> {
                    _accountInfo.value = result.data
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
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
