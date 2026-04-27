package com.signox.dashboard.ui.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.data.model.PairingCodeResponse
import com.signox.dashboard.data.repository.DisplayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplayViewModel @Inject constructor(
    private val displayRepository: DisplayRepository
) : ViewModel() {
    
    private val _displays = MutableLiveData<List<Display>>()
    val displays: LiveData<List<Display>> = _displays
    
    private val _selectedDisplay = MutableLiveData<Display?>()
    val selectedDisplay: LiveData<Display?> = _selectedDisplay
    
    private val _pairingCode = MutableLiveData<PairingCodeResponse?>()
    val pairingCode: LiveData<PairingCodeResponse?> = _pairingCode
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // Filter states
    private val _filterStatus = MutableLiveData<String>("all") // all, online, offline
    val filterStatus: LiveData<String> = _filterStatus
    
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    // Filtered displays based on search and filter
    private val _filteredDisplays = MutableLiveData<List<Display>>()
    val filteredDisplays: LiveData<List<Display>> = _filteredDisplays
    
    // Real-time monitoring
    private var monitoringJob: Job? = null
    private val _isMonitoring = MutableLiveData<Boolean>(false)
    val isMonitoring: LiveData<Boolean> = _isMonitoring
    
    private val MONITORING_INTERVAL = 10_000L // 10 seconds
    
    fun loadDisplays() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.getDisplays().fold(
                onSuccess = { displayList ->
                    _displays.value = displayList
                    applyFilters()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load displays"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun loadDisplay(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.getDisplay(id).fold(
                onSuccess = { display ->
                    _selectedDisplay.value = display
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load display"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun generatePairingCode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.generatePairingCode().fold(
                onSuccess = { response ->
                    _pairingCode.value = response
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to generate pairing code"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun pairDisplay(pairingCode: String, name: String, managedByUserId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.pairDisplay(pairingCode, name, managedByUserId).fold(
                onSuccess = { response ->
                    _successMessage.value = response.message
                    _isLoading.value = false
                    // Reload displays after successful pairing
                    loadDisplays()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to pair display"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun updateDisplay(
        id: String,
        name: String? = null,
        playlistId: String? = null,
        layoutId: String? = null,
        location: String? = null,
        orientation: String? = null,
        tags: List<String>? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.updateDisplay(id, name, playlistId, layoutId, location, orientation, tags).fold(
                onSuccess = { display ->
                    _selectedDisplay.value = display
                    _successMessage.value = "Display updated successfully"
                    _isLoading.value = false
                    // Reload displays to reflect changes
                    loadDisplays()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update display"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun deleteDisplay(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.deleteDisplay(id).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                    _isLoading.value = false
                    // Reload displays after deletion
                    loadDisplays()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete display"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun setFilterStatus(status: String) {
        _filterStatus.value = status
        applyFilters()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    private fun applyFilters() {
        val allDisplays = _displays.value ?: emptyList()
        val status = _filterStatus.value ?: "all"
        val query = _searchQuery.value ?: ""
        
        var filtered = allDisplays
        
        // Apply status filter
        if (status != "all") {
            filtered = filtered.filter { display ->
                display.status.equals(status, ignoreCase = true)
            }
        }
        
        // Apply search query
        if (query.isNotEmpty()) {
            filtered = filtered.filter { display ->
                display.name.contains(query, ignoreCase = true) ||
                display.location?.contains(query, ignoreCase = true) == true ||
                display.pairingCode?.contains(query, ignoreCase = true) == true
            }
        }
        
        _filteredDisplays.value = filtered
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun clearPairingCode() {
        _pairingCode.value = null
    }
    
    fun clearSelectedDisplay() {
        _selectedDisplay.value = null
    }
    
    // Real-time monitoring methods
    fun startMonitoring() {
        if (_isMonitoring.value == true) return
        
        _isMonitoring.value = true
        monitoringJob = viewModelScope.launch {
            while (_isMonitoring.value == true) {
                // Silently refresh displays without showing loading indicator
                displayRepository.getDisplays().fold(
                    onSuccess = { displayList ->
                        val oldDisplays = _displays.value ?: emptyList()
                        _displays.value = displayList
                        applyFilters()
                        
                        // Check for status changes
                        detectStatusChanges(oldDisplays, displayList)
                    },
                    onFailure = {
                        // Silently fail, don't show error during monitoring
                    }
                )
                
                delay(MONITORING_INTERVAL)
            }
        }
    }
    
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    private fun detectStatusChanges(oldDisplays: List<Display>, newDisplays: List<Display>) {
        val oldStatusMap = oldDisplays.associateBy { it.id }
        
        newDisplays.forEach { newDisplay ->
            val oldDisplay = oldStatusMap[newDisplay.id]
            if (oldDisplay != null && oldDisplay.status != newDisplay.status) {
                // Status changed - could show a notification or toast
                android.util.Log.d("DisplayViewModel", 
                    "Display ${newDisplay.name} status changed: ${oldDisplay.status} -> ${newDisplay.status}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
