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
import kotlinx.coroutines.launch
import java.util.Collections

class PlaylistEditorViewModel(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _playlist = MutableLiveData<Playlist?>()
    val playlist: LiveData<Playlist?> = _playlist

    private val _playlistItems = MutableLiveData<List<PlaylistItemWithMedia>>()
    val playlistItems: LiveData<List<PlaylistItemWithMedia>> = _playlistItems

    private val _allMedia = MutableLiveData<List<Media>>()
    val allMedia: LiveData<List<Media>> = _allMedia

    private val _filteredMedia = MutableLiveData<List<Media>>()
    val filteredMedia: LiveData<List<Media>> = _filteredMedia

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentSearchQuery = ""

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                val playlist = playlistRepository.getPlaylistById(playlistId)
                _playlist.value = playlist
                
                loadPlaylistItems(playlistId)
                loadAllMedia()
            } catch (e: Exception) {
                _error.value = "Failed to load playlist: ${e.message}"
            }
        }
    }

    private fun loadPlaylistItems(playlistId: Long) {
        viewModelScope.launch {
            try {
                val items = playlistRepository.getPlaylistItems(playlistId)
                val itemsWithMedia = items.mapNotNull { playlistItem ->
                    val media = mediaRepository.getMediaById(playlistItem.mediaId)
                    media?.let { PlaylistItemWithMedia(playlistItem, it) }
                }.sortedBy { it.playlistItem.position }
                
                _playlistItems.value = itemsWithMedia
            } catch (e: Exception) {
                _error.value = "Failed to load playlist items: ${e.message}"
            }
        }
    }

    private fun loadAllMedia() {
        viewModelScope.launch {
            try {
                val media = mediaRepository.getAllMedia()
                _allMedia.value = media
                filterMedia(currentSearchQuery)
            } catch (e: Exception) {
                _error.value = "Failed to load media: ${e.message}"
            }
        }
    }

    fun searchMedia(query: String) {
        currentSearchQuery = query
        filterMedia(query)
    }

    private fun filterMedia(query: String) {
        val allMedia = _allMedia.value ?: return
        
        if (query.isBlank()) {
            _filteredMedia.value = allMedia
        } else {
            _filteredMedia.value = allMedia.filter { media ->
                media.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun addMediaToPlaylist(media: Media) {
        val currentPlaylist = _playlist.value ?: return
        val currentItems = _playlistItems.value ?: emptyList()
        
        // Check if media is already in playlist
        if (currentItems.any { it.media.id == media.id }) {
            _error.value = "Media is already in the playlist"
            return
        }

        viewModelScope.launch {
            try {
                val newPosition = currentItems.size
                // Set default duration: use media duration if available, otherwise 30 seconds for images
                val defaultDuration = if (media.duration > 0) {
                    media.duration
                } else {
                    30000L // 30 seconds default for images
                }
                
                val playlistItem = PlaylistItem(
                    playlistId = currentPlaylist.id,
                    mediaId = media.id,
                    position = newPosition,
                    duration = defaultDuration
                )
                
                playlistRepository.insertPlaylistItem(playlistItem)
                loadPlaylistItems(currentPlaylist.id)
            } catch (e: Exception) {
                _error.value = "Failed to add media to playlist: ${e.message}"
            }
        }
    }

    fun removeMediaFromPlaylist(playlistItem: PlaylistItem) {
        val currentPlaylist = _playlist.value ?: return
        
        viewModelScope.launch {
            try {
                playlistRepository.deletePlaylistItem(playlistItem)
                // Reorder remaining items
                reorderPlaylistItems(currentPlaylist.id)
                loadPlaylistItems(currentPlaylist.id)
            } catch (e: Exception) {
                _error.value = "Failed to remove media from playlist: ${e.message}"
            }
        }
    }

    fun movePlaylistItem(fromPosition: Int, toPosition: Int) {
        val currentItems = _playlistItems.value?.toMutableList() ?: return
        
        if (fromPosition < 0 || fromPosition >= currentItems.size ||
            toPosition < 0 || toPosition >= currentItems.size) {
            return
        }

        Collections.swap(currentItems, fromPosition, toPosition)
        _playlistItems.value = currentItems

        // Update positions in database
        val currentPlaylist = _playlist.value ?: return
        viewModelScope.launch {
            try {
                currentItems.forEachIndexed { index, itemWithMedia ->
                    val updatedItem = itemWithMedia.playlistItem.copy(position = index)
                    playlistRepository.updatePlaylistItem(updatedItem)
                }
            } catch (e: Exception) {
                _error.value = "Failed to reorder playlist items: ${e.message}"
            }
        }
    }

    fun updateItemDuration(playlistItem: PlaylistItem, newDurationMs: Long) {
        viewModelScope.launch {
            try {
                val updatedItem = playlistItem.copy(duration = newDurationMs)
                playlistRepository.updatePlaylistItem(updatedItem)
                
                // Update the local list without reloading from database
                val currentItems = _playlistItems.value?.toMutableList() ?: return@launch
                val index = currentItems.indexOfFirst { 
                    it.playlistItem.playlistId == playlistItem.playlistId &&
                    it.playlistItem.mediaId == playlistItem.mediaId &&
                    it.playlistItem.position == playlistItem.position
                }
                if (index != -1) {
                    currentItems[index] = currentItems[index].copy(playlistItem = updatedItem)
                    _playlistItems.value = currentItems
                }
            } catch (e: Exception) {
                _error.value = "Failed to update item duration: ${e.message}"
            }
        }
    }

    private suspend fun reorderPlaylistItems(playlistId: Long) {
        val items = playlistRepository.getPlaylistItems(playlistId)
        items.sortedBy { it.position }.forEachIndexed { index, item ->
            val updatedItem = item.copy(position = index)
            playlistRepository.updatePlaylistItem(updatedItem)
        }
    }

    fun savePlaylist() {
        val currentPlaylist = _playlist.value ?: return
        val items = _playlistItems.value ?: emptyList()
        
        viewModelScope.launch {
            try {
                val totalDuration = items.sumOf { it.playlistItem.duration }
                val updatedPlaylist = currentPlaylist.copy(
                    totalDuration = totalDuration,
                    updatedAt = System.currentTimeMillis()
                )
                playlistRepository.updatePlaylist(updatedPlaylist)
            } catch (e: Exception) {
                _error.value = "Failed to save playlist: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    data class PlaylistItemWithMedia(
        val playlistItem: PlaylistItem,
        val media: Media
    )
}