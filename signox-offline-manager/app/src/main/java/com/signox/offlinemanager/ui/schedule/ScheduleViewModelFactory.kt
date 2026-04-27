package com.signox.offlinemanager.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository

class ScheduleViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
    private val playlistRepository: PlaylistRepository,
    private val layoutRepository: LayoutRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            return ScheduleViewModel(scheduleRepository, playlistRepository, layoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}