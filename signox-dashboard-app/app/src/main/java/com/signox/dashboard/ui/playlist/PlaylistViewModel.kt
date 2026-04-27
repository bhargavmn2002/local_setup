package com.signox.dashboard.ui.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.dashboard.data.model.Display
import com.signox.dashboard.data.model.Playlist
import com.signox.dashboard.data.model.PlaylistItemRequest
import com.signox.dashboard.data.repository.DisplayRepository
import com.signox.dashboard.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val displayRepository: DisplayRepository
) : ViewModel() {
    
    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> = _playlists
    
    private val _currentPlaylist = MutableLiveData<Playlist?>()
    val currentPlaylist: LiveData<Playlist?> = _currentPlaylist
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // For display assignment
    private val _displays = MutableLiveData<List<Display>>()
    val displays: LiveData<List<Display>> = _displays
    
    private val _assignmentSuccess = MutableLiveData<Boolean>()
    val assignmentSuccess: LiveData<Boolean> = _assignmentSuccess
    
    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            playlistRepository.getPlaylists()
                .onSuccess { playlists ->
                    _playlists.value = playlists
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load playlists"
                    _isLoading.value = false
                }
        }
    }
    
    fun loadPlaylist(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            playlistRepository.getPlaylist(id)
                .onSuccess { playlist ->
                    _currentPlaylist.value = playlist
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load playlist"
                    _isLoading.value = false
                }
        }
    }
    
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            playlistRepository.createPlaylist(name)
                .onSuccess { playlist ->
                    _successMessage.value = "Playlist created successfully"
                    _currentPlaylist.value = playlist
                    _isLoading.value = false
                    loadPlaylists() // Refresh list
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to create playlist"
                    _isLoading.value = false
                }
        }
    }
    
    fun updatePlaylist(id: String, name: String, items: List<PlaylistItemRequest>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            playlistRepository.updatePlaylist(id, name, items)
                .onSuccess { playlist ->
                    _successMessage.value = "Playlist updated successfully"
                    _currentPlaylist.value = playlist
                    _isLoading.value = false
                    loadPlaylists() // Refresh list
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to update playlist"
                    _isLoading.value = false
                }
        }
    }
    
    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            playlistRepository.deletePlaylist(id)
                .onSuccess { message ->
                    _successMessage.value = message
                    _isLoading.value = false
                    loadPlaylists() // Refresh list
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to delete playlist"
                    _isLoading.value = false
                }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun clearCurrentPlaylist() {
        _currentPlaylist.value = null
    }
    
    // Display assignment methods
    fun loadDisplays() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            displayRepository.getDisplays()
                .onSuccess { displays ->
                    _displays.value = displays
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load displays"
                    _isLoading.value = false
                }
        }
    }
    
    fun assignPlaylistToDisplays(playlistId: String, displayIds: Set<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()
            
            // Process each display sequentially to ensure proper counting
            for (displayId in displayIds) {
                try {
                    displayRepository.updateDisplay(displayId, playlistId = playlistId)
                        .onSuccess {
                            successCount++
                        }
                        .onFailure { exception ->
                            failCount++
                            errors.add(exception.message ?: "Unknown error")
                        }
                } catch (e: Exception) {
                    failCount++
                    errors.add(e.message ?: "Unknown error")
                }
            }
            
            _isLoading.value = false
            
            if (failCount == 0) {
                _successMessage.value = "Playlist assigned to $successCount display(s)"
                _assignmentSuccess.value = true
            } else if (successCount > 0) {
                _successMessage.value = "Assigned to $successCount display(s), $failCount failed"
                _assignmentSuccess.value = true
            } else {
                _error.value = "Failed to assign playlist to displays: ${errors.firstOrNull() ?: "Unknown error"}"
                _assignmentSuccess.value = false
            }
        }
    }
    
    fun unassignPlaylistFromDisplays(displayIds: Set<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()
            
            // Process each display sequentially to ensure proper counting
            for (displayId in displayIds) {
                try {
                    displayRepository.updateDisplay(displayId, playlistId = null)
                        .onSuccess {
                            successCount++
                        }
                        .onFailure { exception ->
                            failCount++
                            errors.add(exception.message ?: "Unknown error")
                        }
                } catch (e: Exception) {
                    failCount++
                    errors.add(e.message ?: "Unknown error")
                }
            }
            
            _isLoading.value = false
            
            if (failCount == 0) {
                _successMessage.value = "Playlist unassigned from $successCount display(s)"
                _assignmentSuccess.value = true
            } else if (successCount > 0) {
                _successMessage.value = "Unassigned from $successCount display(s), $failCount failed"
                _assignmentSuccess.value = true
            } else {
                _error.value = "Failed to unassign playlist from displays: ${errors.firstOrNull() ?: "Unknown error"}"
                _assignmentSuccess.value = false
            }
        }
    }
}
