package com.signox.offlinemanager.ui.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.model.PlaylistItem
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.utils.AppMonitor
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _playlists = MutableLiveData<List<PlaylistWithDetails>>()
    val playlists: LiveData<List<PlaylistWithDetails>> = _playlists

    private val _filteredPlaylists = MutableLiveData<List<PlaylistWithDetails>>()
    val filteredPlaylists: LiveData<List<PlaylistWithDetails>> = _filteredPlaylists

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentSearchQuery = ""

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val playlistsWithDetails = playlistRepository.getAllPlaylists().map { playlist ->
                    val items = playlistRepository.getPlaylistItems(playlist.id)
                    val itemCount = items.size
                    val totalDuration = items.sumOf { it.duration }
                    
                    PlaylistWithDetails(
                        playlist = playlist,
                        itemCount = itemCount,
                        totalDuration = totalDuration
                    )
                }
                _playlists.value = playlistsWithDetails
                filterPlaylists(currentSearchQuery)
            } catch (e: Exception) {
                _error.value = "Failed to load playlists: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchPlaylists(query: String) {
        currentSearchQuery = query
        filterPlaylists(query)
    }

    private fun filterPlaylists(query: String) {
        val allPlaylists = _playlists.value ?: return
        
        if (query.isBlank()) {
            _filteredPlaylists.value = allPlaylists
        } else {
            _filteredPlaylists.value = allPlaylists.filter { playlistWithDetails ->
                playlistWithDetails.playlist.name.contains(query, ignoreCase = true) ||
                playlistWithDetails.playlist.description?.contains(query, ignoreCase = true) == true
            }
        }
    }

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            try {
                AppMonitor.logDatabaseOperation("CREATE", "Playlist", "name: $name, description: $description")
                val playlist = Playlist(
                    name = name,
                    description = description
                )
                playlistRepository.insertPlaylist(playlist)
                AppMonitor.logDatabaseOperation("CREATE", "Playlist", "SUCCESS - Playlist created")
                loadPlaylists()
            } catch (e: Exception) {
                AppMonitor.logError("PlaylistViewModel.createPlaylist", e)
                _error.value = "Failed to create playlist: ${e.message}"
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                playlistRepository.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
                loadPlaylists()
            } catch (e: Exception) {
                _error.value = "Failed to update playlist: ${e.message}"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.deletePlaylist(playlistId)
                loadPlaylists()
            } catch (e: Exception) {
                _error.value = "Failed to delete playlist: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    data class PlaylistWithDetails(
        val playlist: Playlist,
        val itemCount: Int,
        val totalDuration: Long
    )
}