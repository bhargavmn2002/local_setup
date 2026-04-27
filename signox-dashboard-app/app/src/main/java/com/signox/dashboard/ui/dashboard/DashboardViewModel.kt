package com.signox.dashboard.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.api.DashboardSummary
import com.signox.dashboard.data.api.NetworkResult
import com.signox.dashboard.data.model.ProfileResponse
import com.signox.dashboard.data.repository.AuthRepository
import com.signox.dashboard.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _profileState = MutableLiveData<NetworkResult<ProfileResponse>>()
    val profileState: LiveData<NetworkResult<ProfileResponse>> = _profileState
    
    private val _summaryState = MutableLiveData<NetworkResult<DashboardSummary>>()
    val summaryState: LiveData<NetworkResult<DashboardSummary>> = _summaryState
    
    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent
    
    fun loadDashboardData() {
        loadProfile()
        loadSummary()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = NetworkResult.Loading()
            _profileState.value = dashboardRepository.getUserProfile()
        }
    }
    
    private fun loadSummary() {
        viewModelScope.launch {
            _summaryState.value = NetworkResult.Loading()
            _summaryState.value = dashboardRepository.getDashboardSummary()
        }
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutEvent.value = true
        }
    }
}
