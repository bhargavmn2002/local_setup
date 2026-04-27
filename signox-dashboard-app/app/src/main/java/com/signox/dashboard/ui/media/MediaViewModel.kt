package com.signox.dashboard.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.Media
import com.signox.dashboard.data.model.StorageInfo
import com.signox.dashboard.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _mediaList = MutableLiveData<List<Media>>()
    val mediaList: LiveData<List<Media>> = _mediaList
    
    private val _selectedMedia = MutableLiveData<Media?>()
    val selectedMedia: LiveData<Media?> = _selectedMedia
    
    private val _storageInfo = MutableLiveData<StorageInfo?>()
    val storageInfo: LiveData<StorageInfo?> = _storageInfo
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // Filter states
    private val _filterType = MutableLiveData<String>("all") // all, image, video
    val filterType: LiveData<String> = _filterType
    
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    // Filtered media based on search and filter
    private val _filteredMedia = MutableLiveData<List<Media>>()
    val filteredMedia: LiveData<List<Media>> = _filteredMedia
    
    // Pagination
    private var currentPage = 1
    private val pageSize = 20
    private var hasMorePages = true
    
    fun loadMedia(refresh: Boolean = false) {
        // Prevent loading if already loading
        if (_isLoading.value == true && !refresh) return
        
        if (refresh) {
            currentPage = 1
            hasMorePages = true
        }
        
        viewModelScope.launch {
            // Clear error state before starting new request
            _error.value = null
            _isLoading.value = true
            
            val typeFilter = when (_filterType.value) {
                "image" -> "IMAGE"
                "video" -> "VIDEO"
                else -> null
            }
            
            try {
                val result = mediaRepository.getMedia(
                    page = currentPage,
                    limit = pageSize,
                    search = _searchQuery.value?.takeIf { it.isNotEmpty() },
                    type = typeFilter
                )
                
                result.fold(
                    onSuccess = { response ->
                        val newMedia = response.mediaList
                        
                        if (refresh) {
                            _mediaList.value = newMedia
                        } else {
                            val currentList = _mediaList.value ?: emptyList()
                            _mediaList.value = currentList + newMedia
                        }
                        
                        // Update storage info if available
                        response.storageInfo?.let { storage ->
                            _storageInfo.value = storage
                        }
                        
                        response.pagination?.let { pagination ->
                            hasMorePages = currentPage < pagination.totalPages
                        }
                        
                        applyFilters()
                        _error.value = null // Explicitly clear error on success
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load media"
                        _isLoading.value = false
                        // Don't clear the existing list on error
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("MediaViewModel", "Exception in loadMedia", e)
                _error.value = e.message ?: "Failed to load media"
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreMedia() {
        if (!hasMorePages || _isLoading.value == true) return
        
        currentPage++
        loadMedia(refresh = false)
    }
    
    fun loadStorageInfo() {
        viewModelScope.launch {
            mediaRepository.getStorageInfo().fold(
                onSuccess = { storage ->
                    _storageInfo.value = storage
                },
                onFailure = { exception ->
                    // Don't show error for storage info, it's not critical
                }
            )
        }
    }
    
    fun uploadMedia(file: File, name: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            mediaRepository.uploadMedia(file, name).fold(
                onSuccess = { response ->
                    _successMessage.value = response.message
                    _isLoading.value = false
                    // Reload media list and storage info
                    loadMedia(refresh = true)
                    loadStorageInfo()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to upload media"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun updateMedia(
        id: String,
        name: String? = null,
        description: String? = null,
        tags: List<String>? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            mediaRepository.updateMedia(id, name, description, tags).fold(
                onSuccess = { media ->
                    _selectedMedia.value = media
                    _successMessage.value = "Media updated successfully"
                    _isLoading.value = false
                    // Reload media list
                    loadMedia(refresh = true)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update media"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun updateMediaWithExpiry(id: String, expiryDate: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            mediaRepository.updateMediaExpiry(id, expiryDate).fold(
                onSuccess = { media ->
                    _selectedMedia.value = media
                    _successMessage.value = if (expiryDate != null) {
                        "Expiry date set successfully"
                    } else {
                        "Expiry date removed"
                    }
                    _isLoading.value = false
                    // Reload media list
                    loadMedia(refresh = true)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update expiry date"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun deleteMedia(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            mediaRepository.deleteMedia(id).fold(
                onSuccess = { message ->
                    _successMessage.value = message
                    _isLoading.value = false
                    // Reload media list and storage info
                    loadMedia(refresh = true)
                    loadStorageInfo()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete media"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun setFilterType(type: String) {
        if (_filterType.value == type) return // Skip if same filter
        _filterType.value = type
        loadMedia(refresh = true)
    }
    
    fun setSearchQuery(query: String) {
        if (_searchQuery.value == query) return // Skip if same query
        _searchQuery.value = query
        loadMedia(refresh = true)
    }
    
    private fun applyFilters() {
        // Filters are applied server-side, so just update the filtered list
        _filteredMedia.value = _mediaList.value ?: emptyList()
    }
    
    fun selectMedia(media: Media) {
        _selectedMedia.value = media
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun clearSelectedMedia() {
        _selectedMedia.value = null
    }
}
