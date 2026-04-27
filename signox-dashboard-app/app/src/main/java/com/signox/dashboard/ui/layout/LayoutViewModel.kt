package com.signox.dashboard.ui.layout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.*
import com.signox.dashboard.data.repository.LayoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LayoutViewModel @Inject constructor(
    private val repository: LayoutRepository
) : ViewModel() {
    
    private val _layouts = MutableLiveData<List<Layout>>()
    val layouts: LiveData<List<Layout>> = _layouts
    
    private val _currentLayout = MutableLiveData<Layout?>()
    val currentLayout: LiveData<Layout?> = _currentLayout
    
    private val _mediaList = MutableLiveData<List<Media>>()
    val mediaList: LiveData<List<Media>> = _mediaList
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success
    
    // Search query for filtering layouts
    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery
    
    private var allLayouts: List<Layout> = emptyList()
    
    fun loadLayouts() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.getLayouts().fold(
                onSuccess = { layoutList ->
                    allLayouts = layoutList
                    applySearch()
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load layouts"
                    _loading.value = false
                }
            )
        }
    }
    
    fun loadLayout(id: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.getLayout(id).fold(
                onSuccess = { layout ->
                    _currentLayout.value = layout
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load layout"
                    _loading.value = false
                }
            )
        }
    }
    
    fun createLayout(request: CreateLayoutRequest, onSuccess: (Layout) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.createLayout(request).fold(
                onSuccess = { layout ->
                    _success.value = "Layout created successfully"
                    _loading.value = false
                    onSuccess(layout)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to create layout"
                    _loading.value = false
                }
            )
        }
    }
    
    fun updateLayout(id: String, request: UpdateLayoutRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.updateLayout(id, request).fold(
                onSuccess = { layout ->
                    _currentLayout.value = layout
                    _success.value = "Layout updated successfully"
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update layout"
                    _loading.value = false
                }
            )
        }
    }
    
    fun deleteLayout(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.deleteLayout(id).fold(
                onSuccess = { message ->
                    _success.value = message
                    _loading.value = false
                    onSuccess()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete layout"
                    _loading.value = false
                }
            )
        }
    }
    
    fun updateSection(layoutId: String, sectionId: String, request: UpdateSectionRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            repository.updateSection(layoutId, sectionId, request).fold(
                onSuccess = { layout ->
                    _currentLayout.value = layout
                    _success.value = "Section updated successfully"
                    _loading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update section"
                    _loading.value = false
                }
            )
        }
    }
    
    fun loadMedia() {
        viewModelScope.launch {
            android.util.Log.d("LayoutViewModel", "Loading media...")
            repository.getMedia().fold(
                onSuccess = { media ->
                    android.util.Log.d("LayoutViewModel", "Media loaded successfully: ${media.size} items")
                    _mediaList.value = media
                },
                onFailure = { exception ->
                    android.util.Log.e("LayoutViewModel", "Failed to load media", exception)
                    _error.value = exception.message ?: "Failed to load media"
                }
            )
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applySearch()
    }
    
    private fun applySearch() {
        val query = _searchQuery.value?.lowercase() ?: ""
        _layouts.value = if (query.isEmpty()) {
            allLayouts
        } else {
            allLayouts.filter { layout ->
                layout.name.lowercase().contains(query)
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccess() {
        _success.value = null
    }
    
    fun clearCurrentLayout() {
        _currentLayout.value = null
    }
}
