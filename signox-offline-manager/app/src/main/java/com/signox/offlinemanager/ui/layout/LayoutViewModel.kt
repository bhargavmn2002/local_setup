package com.signox.offlinemanager.ui.layout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.repository.LayoutRepository
import kotlinx.coroutines.launch

class LayoutViewModel(
    private val layoutRepository: LayoutRepository
) : ViewModel() {
    
    val layouts: LiveData<List<Layout>> = layoutRepository.getAllLayouts()
    
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun createLayout(name: String, description: String, width: Int = 1920, height: Int = 1080, template: LayoutTemplate? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Debug logging
                android.util.Log.d("LayoutViewModel", "Creating layout: $name, ${width}x${height}, template: ${template?.name}")
                
                val layout = Layout(
                    name = name,
                    description = description,
                    width = width,
                    height = height
                )
                
                val layoutId = layoutRepository.createLayout(layout)
                android.util.Log.d("LayoutViewModel", "Layout created with ID: $layoutId")
                
                // If template is provided, create zones from template sections
                template?.let { tmpl ->
                    android.util.Log.d("LayoutViewModel", "Creating ${tmpl.sections.size} zones from template")
                    createZonesFromTemplate(layoutId, tmpl, width, height)
                }
                
                android.util.Log.d("LayoutViewModel", "Layout creation completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("LayoutViewModel", "Failed to create layout", e)
                _error.value = e.message ?: "Failed to create layout"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun createZonesFromTemplate(layoutId: Long, template: LayoutTemplate, layoutWidth: Int, layoutHeight: Int) {
        template.sections.forEach { section ->
            val zone = com.signox.offlinemanager.data.model.LayoutZone(
                layoutId = layoutId,
                name = section.name,
                x = (section.x * layoutWidth / 100).toInt(),
                y = (section.y * layoutHeight / 100).toInt(),
                width = (section.width * layoutWidth / 100).toInt(),
                height = (section.height * layoutHeight / 100).toInt(),
                zIndex = section.order
            )
            android.util.Log.d("LayoutViewModel", "Creating zone: ${zone.name} at (${zone.x}, ${zone.y}) size ${zone.width}x${zone.height}")
            layoutRepository.createLayoutZone(zone)
        }
    }
    
    fun deleteLayout(layout: Layout) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                layoutRepository.deleteLayout(layout)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete layout"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}