package com.signox.dashboard.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.model.LoginResponse
import com.signox.dashboard.data.model.UserRole
import com.signox.dashboard.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginState: LiveData<NetworkResult<LoginResponse>> = _loginState
    
    private val _navigationEvent = MutableLiveData<Pair<UserRole, String?>?>()
    val navigationEvent: LiveData<Pair<UserRole, String?>?> = _navigationEvent
    
    fun login(email: String, password: String) {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = NetworkResult.Error("Email and password are required")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = NetworkResult.Error("Invalid email format")
            return
        }
        
        viewModelScope.launch {
            _loginState.value = NetworkResult.Loading()
            
            val result = authRepository.login(email, password)
            _loginState.value = result
            
            // Trigger navigation on success
            if (result is NetworkResult.Success) {
                result.data?.user?.let { user ->
                    _navigationEvent.value = Pair(user.role, user.staffRole?.name)
                }
            }
        }
    }
    
    fun resetNavigationEvent() {
        _navigationEvent.value = null
    }
}
