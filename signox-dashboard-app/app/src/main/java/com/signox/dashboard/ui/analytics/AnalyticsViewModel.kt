package com.signox.dashboard.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.AnalyticsSummary
import com.signox.dashboard.data.model.PlaybackLog
import com.signox.dashboard.data.model.Report
import com.signox.dashboard.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {
    
    private val _analyticsSummary = MutableStateFlow<AnalyticsSummary?>(null)
    val analyticsSummary: StateFlow<AnalyticsSummary?> = _analyticsSummary.asStateFlow()
    
    private val _proofOfPlayLogs = MutableStateFlow<List<PlaybackLog>>(emptyList())
    val proofOfPlayLogs: StateFlow<List<PlaybackLog>> = _proofOfPlayLogs.asStateFlow()
    
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadAnalyticsSummary(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getAnalyticsSummary(startDate, endDate)
                .onSuccess { summary ->
                    _analyticsSummary.value = summary
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load analytics"
                    _isLoading.value = false
                }
        }
    }
    
    fun loadProofOfPlay(displayId: String? = null, startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getProofOfPlay(displayId, startDate, endDate)
                .onSuccess { logs ->
                    _proofOfPlayLogs.value = logs
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load proof of play"
                    _isLoading.value = false
                }
        }
    }
    
    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getReports()
                .onSuccess { reportsList ->
                    _reports.value = reportsList
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load reports"
                    _isLoading.value = false
                }
        }
    }
    
    fun generateReport(
        name: String,
        type: String,
        startDate: String,
        endDate: String,
        displays: List<String>? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.generateReport(name, type, startDate, endDate, displays)
                .onSuccess {
                    loadReports() // Reload reports after generating
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to generate report"
                    _isLoading.value = false
                }
        }
    }
    
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.deleteReport(reportId)
                .onSuccess {
                    loadReports() // Reload reports after deleting
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to delete report"
                    _isLoading.value = false
                }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
