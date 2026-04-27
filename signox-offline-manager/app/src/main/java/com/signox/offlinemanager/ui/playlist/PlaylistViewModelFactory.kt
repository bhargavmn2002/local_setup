package com.signox.offlinemanager.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository

class PlaylistViewModelFactory(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            return PlaylistViewModel(playlistRepository, mediaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}