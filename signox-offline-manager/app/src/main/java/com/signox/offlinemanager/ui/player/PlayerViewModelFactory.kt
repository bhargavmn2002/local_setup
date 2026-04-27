package com.signox.offlinemanager.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository

class PlayerViewModelFactory(
    private val playlistRepository: PlaylistRepository,
    private val layoutRepository: LayoutRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(playlistRepository, layoutRepository, scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}