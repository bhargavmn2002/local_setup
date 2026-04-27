package com.signox.offlinemanager.ui.layout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.LayoutZone
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.MediaRepository
import kotlinx.coroutines.launch

class LayoutEditorViewModel(
    private val layoutRepository: LayoutRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _layout = MutableLiveData<Layout?>()
    val layout: LiveData<Layout?> = _layout
    
    private val _zones = MutableLiveData<List<LayoutZone>>(emptyList())
    val zones: LiveData<List<LayoutZone>> = _zones
    
    private val _selectedZone = MutableLiveData<LayoutZone?>()
    val selectedZone: LiveData<LayoutZone?> = _selectedZone
    
    private val _selectedMedia = MutableLiveData<Media?>()
    val selectedMedia: LiveData<Media?> = _selectedMedia
    
    val mediaList: LiveData<List<Media>> = mediaRepository.getAllMediaLive()
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var currentLayoutId: Long = -1
    
    fun loadLayout(layoutId: Long) {
        currentLayoutId = layoutId
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val layout = layoutRepository.getLayoutById(layoutId)
                _layout.value = layout
                
                // Observe zones for this layout
                layoutRepository.getLayoutZonesLiveData(layoutId).observeForever { zones ->
                    _zones.value = zones
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load layout"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectZone(zone: LayoutZone?) {
        _selectedZone.value = zone
    }
    
    fun selectMedia(media: Media?) {
        _selectedMedia.value = media
    }
    
    fun assignMediaToZone(zone: LayoutZone, media: Media?) {
        viewModelScope.launch {
            try {
                val updatedZone = zone.copy(
                    mediaId = media?.id,
                    playlistId = null // Clear playlist if media is assigned
                )
                
                layoutRepository.updateLayoutZone(updatedZone)
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to assign media to zone"
            }
        }
    }
    
    fun deleteZone(zone: LayoutZone) {
        viewModelScope.launch {
            try {
                layoutRepository.deleteLayoutZone(zone)
                
                // Clear selection if deleted zone was selected
                if (_selectedZone.value?.id == zone.id) {
                    _selectedZone.value = null
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete zone"
            }
        }
    }
    
    fun updateZone(zone: LayoutZone) {
        viewModelScope.launch {
            try {
                layoutRepository.updateLayoutZone(zone)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update zone"
            }
        }
    }
    
    fun saveLayout() {
        val currentLayout = _layout.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val updatedLayout = currentLayout.copy(
                    updatedAt = System.currentTimeMillis()
                )
                
                layoutRepository.updateLayout(updatedLayout)
                
                _error.value = "Layout saved successfully"
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save layout"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}