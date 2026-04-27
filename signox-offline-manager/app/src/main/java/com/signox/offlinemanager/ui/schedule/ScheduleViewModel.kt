package com.signox.offlinemanager.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.model.ContentType
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val playlistRepository: PlaylistRepository,
    private val layoutRepository: LayoutRepository
) : ViewModel() {
    
    val schedules: LiveData<List<Schedule>> = scheduleRepository.getAllSchedulesLiveData()
    val activeSchedules: LiveData<List<Schedule>> = scheduleRepository.getActiveSchedules()
    val playlists: LiveData<List<Playlist>> = playlistRepository.getAllPlaylistsLiveData()
    val layouts: LiveData<List<Layout>> = layoutRepository.getAllLayouts()
    
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _conflictError = MutableLiveData<String?>()
    val conflictError: LiveData<String?> = _conflictError
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun createSchedule(
        name: String,
        description: String?,
        startTime: String,
        endTime: String,
        daysOfWeek: String,
        contentType: ContentType,
        contentId: Long
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _conflictError.value = null
                
                // Check for conflicts
                val hasConflict = scheduleRepository.hasConflict(startTime, endTime, daysOfWeek)
                if (hasConflict) {
                    _conflictError.value = "Schedule conflicts with existing schedule"
                    return@launch
                }
                
                val schedule = Schedule(
                    name = name,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    daysOfWeek = daysOfWeek,
                    contentType = contentType,
                    contentId = contentId
                )
                
                scheduleRepository.createSchedule(schedule)
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create schedule"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateSchedule(
        scheduleId: Long,
        name: String,
        description: String?,
        startTime: String,
        endTime: String,
        daysOfWeek: String,
        contentType: ContentType,
        contentId: Long,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _conflictError.value = null
                
                // Check for conflicts (excluding current schedule)
                val hasConflict = scheduleRepository.hasConflict(
                    startTime, endTime, daysOfWeek, scheduleId
                )
                if (hasConflict) {
                    _conflictError.value = "Schedule conflicts with existing schedule"
                    return@launch
                }
                
                val existingSchedule = scheduleRepository.getScheduleById(scheduleId)
                if (existingSchedule != null) {
                    val updatedSchedule = existingSchedule.copy(
                        name = name,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        daysOfWeek = daysOfWeek,
                        contentType = contentType,
                        contentId = contentId,
                        isActive = isActive,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    scheduleRepository.updateSchedule(updatedSchedule)
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update schedule"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleScheduleStatus(schedule: Schedule) {
        viewModelScope.launch {
            try {
                scheduleRepository.toggleScheduleStatus(schedule)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to toggle schedule status"
            }
        }
    }
    
    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                scheduleRepository.deleteSchedule(schedule)
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete schedule"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearConflictError() {
        _conflictError.value = null
    }
    
    // Helper methods for UI
    fun formatDaysOfWeek(daysOfWeek: String): String {
        val dayNames = mapOf(
            1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
            5 to "Fri", 6 to "Sat", 7 to "Sun"
        )
        
        return daysOfWeek.split(",")
            .mapNotNull { dayNames[it.toIntOrNull()] }
            .joinToString(", ")
    }
    
    fun getContentName(contentType: ContentType, contentId: Long): String {
        return when (contentType) {
            ContentType.PLAYLIST -> {
                playlists.value?.find { it.id == contentId }?.name ?: "Unknown Playlist"
            }
            ContentType.LAYOUT -> {
                layouts.value?.find { it.id == contentId }?.name ?: "Unknown Layout"
            }
        }
    }
}