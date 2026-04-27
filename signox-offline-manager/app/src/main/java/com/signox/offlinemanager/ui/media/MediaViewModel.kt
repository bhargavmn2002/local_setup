package com.signox.offlinemanager.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.database.AppDatabase
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.MediaType
import com.signox.offlinemanager.data.repository.MediaRepository
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    
    private val mediaRepository = MediaRepository(AppDatabase.getDatabase(application).mediaDao())
    
    private val _mediaList = MutableLiveData<List<Media>>()
    val mediaList: LiveData<List<Media>> = _mediaList
    
    private val _filteredMediaList = MutableLiveData<List<Media>>()
    val filteredMediaList: LiveData<List<Media>> = _filteredMediaList
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var currentFilter = MediaType.values().toList()
    private var currentSearchQuery = ""
    
    init {
        loadMedia()
    }
    
    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val media = mediaRepository.getAllMedia()
                _mediaList.value = media
                applyFilters()
            } catch (e: Exception) {
                _error.value = "Failed to load media: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addMedia(media: Media) {
        viewModelScope.launch {
            try {
                mediaRepository.insertMedia(media)
                loadMedia() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to add media: ${e.message}"
            }
        }
    }
    
    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            try {
                // Delete the actual file
                val file = File(media.filePath)
                if (file.exists()) {
                    file.delete()
                }
                
                // Delete thumbnail if exists
                media.thumbnailPath?.let { thumbnailPath ->
                    val thumbnailFile = File(thumbnailPath)
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete()
                    }
                }
                
                // Delete from database
                mediaRepository.deleteMedia(media)
                loadMedia() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Failed to delete media: ${e.message}"
            }
        }
    }
    
    fun searchMedia(query: String) {
        currentSearchQuery = query
        applyFilters()
    }
    
    fun filterByType(types: List<MediaType>) {
        currentFilter = types
        applyFilters()
    }
    
    private fun applyFilters() {
        val allMedia = _mediaList.value ?: return
        
        var filtered = allMedia
        
        // Apply type filter
        if (currentFilter.size < MediaType.values().size) {
            filtered = filtered.filter { it.type in currentFilter }
        }
        
        // Apply search filter
        if (currentSearchQuery.isNotBlank()) {
            filtered = filtered.filter { 
                it.name.contains(currentSearchQuery, ignoreCase = true)
            }
        }
        
        _filteredMediaList.value = filtered
    }
    
    fun clearError() {
        _error.value = null
    }
}