package com.signox.dashboard.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.*
import com.signox.dashboard.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {
    
    private val _schedules = MutableLiveData<List<Schedule>>()
    val schedules: LiveData<List<Schedule>> = _schedules
    
    private val _currentSchedule = MutableLiveData<Schedule?>()
    val currentSchedule: LiveData<Schedule?> = _currentSchedule
    
    private val _activeSchedules = MutableLiveData<ActiveSchedulesResponse?>()
    val activeSchedules: LiveData<ActiveSchedulesResponse?> = _activeSchedules
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success
    
    // Search query for filtering schedules
    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery
    
    private var allSchedules: List<Schedule> = emptyList()
    
    fun loadSchedules() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.getSchedules().fold(
                onSuccess = { scheduleList ->
                    allSchedules = scheduleList
                    applySearch()
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load schedules"
                    _loading.value = false
                }
            )
        }
    }
    
    fun loadActiveSchedules(displayId: String? = null, timezone: String = "Asia/Kolkata") {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.getActiveSchedules(displayId, timezone).fold(
                onSuccess = { response ->
                    _activeSchedules.value = response
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load active schedules"
                    _loading.value = false
                }
            )
        }
    }
    
    fun loadSchedule(id: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            // Find schedule in the list first
            val schedule = allSchedules.find { it.id == id }
            if (schedule != null) {
                _currentSchedule.value = schedule
                _loading.value = false
            } else {
                // If not in list, fetch from API
                repository.getSchedules().fold(
                    onSuccess = { schedules ->
                        val foundSchedule = schedules.find { it.id == id }
                        if (foundSchedule != null) {
                            _currentSchedule.value = foundSchedule
                        } else {
                            _error.value = "Schedule not found"
                        }
                        _loading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load schedule"
                        _loading.value = false
                    }
                )
            }
        }
    }
    
    fun createSchedule(request: CreateScheduleRequest, onSuccess: (Schedule) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.createSchedule(request).fold(
                onSuccess = { schedule ->
                    _success.value = "Schedule created successfully"
                    _loading.value = false
                    onSuccess(schedule)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to create schedule"
                    _loading.value = false
                }
            )
        }
    }
    
    fun updateSchedule(id: String, request: UpdateScheduleRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.updateSchedule(id, request).fold(
                onSuccess = { schedule ->
                    _currentSchedule.value = schedule
                    _success.value = "Schedule updated successfully"
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update schedule"
                    _loading.value = false
                }
            )
        }
    }
    
    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.deleteSchedule(id).fold(
                onSuccess = {
                    _success.value = "Schedule deleted successfully"
                    _loading.value = false
                    loadSchedules() // Reload list
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete schedule"
                    _loading.value = false
                }
            )
        }
    }
    
    fun toggleScheduleActive(schedule: Schedule) {
        val request = UpdateScheduleRequest(
            name = null,
            description = null,
            startTime = null,
            endTime = null,
            timezone = null,
            repeatDays = null,
            startDate = null,
            endDate = null,
            priority = null,
            playlistId = null,
            layoutId = null,
            displayIds = null,
            isActive = !schedule.isActive,
            orientation = null
        )
        updateSchedule(schedule.id, request)
    }
    
    fun searchSchedules(query: String) {
        _searchQuery.value = query
        applySearch()
    }
    
    private fun applySearch() {
        val query = _searchQuery.value?.lowercase() ?: ""
        _schedules.value = if (query.isEmpty()) {
            allSchedules
        } else {
            allSchedules.filter { schedule ->
                schedule.name.lowercase().contains(query) ||
                schedule.description?.lowercase()?.contains(query) == true ||
                schedule.playlist?.name?.lowercase()?.contains(query) == true ||
                schedule.layout?.name?.lowercase()?.contains(query) == true
            }
        }
    }
    
    fun setCurrentSchedule(schedule: Schedule?) {
        _currentSchedule.value = schedule
    }
    
    fun clearCurrentSchedule() {
        _currentSchedule.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccess() {
        _success.value = null
    }
}
