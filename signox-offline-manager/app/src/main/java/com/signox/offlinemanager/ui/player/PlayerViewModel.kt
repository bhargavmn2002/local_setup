package com.signox.offlinemanager.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signox.offlinemanager.data.model.ContentType
import com.signox.offlinemanager.data.model.Schedule
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PlayerViewModel(
    private val playlistRepository: PlaylistRepository,
    private val layoutRepository: LayoutRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _contentItems = MediatorLiveData<List<PlayerContentItem>>()
    val contentItems: LiveData<List<PlayerContentItem>> = _contentItems

    private val _currentSchedule = MutableLiveData<Schedule?>()
    val currentSchedule: LiveData<Schedule?> = _currentSchedule

    private val playlistsLiveData = playlistRepository.getAllPlaylistsLiveData()
    private val layoutsLiveData = layoutRepository.getAllLayoutsLiveData()

    init {
        setupContentItems()
        checkCurrentSchedule()
    }

    private fun setupContentItems() {
        _contentItems.addSource(playlistsLiveData) { playlists ->
            combineContentItems()
        }
        _contentItems.addSource(layoutsLiveData) { layouts ->
            combineContentItems()
        }
    }

    private fun combineContentItems() {
        val playlists = playlistsLiveData.value ?: emptyList()
        val layouts = layoutsLiveData.value ?: emptyList()

        val items = mutableListOf<PlayerContentItem>()

        // Add playlists
        playlists.forEach { playlist ->
            items.add(
                PlayerContentItem(
                    id = playlist.id,
                    name = playlist.name,
                    description = playlist.description ?: "No description",
                    type = PlayerContentType.PLAYLIST,
                    duration = playlist.totalDuration,
                    createdAt = playlist.createdAt
                )
            )
        }

        // Add layouts
        layouts.forEach { layout ->
            items.add(
                PlayerContentItem(
                    id = layout.id,
                    name = layout.name,
                    description = layout.description ?: "No description",
                    type = PlayerContentType.LAYOUT,
                    duration = 0, // Layouts don't have fixed duration
                    createdAt = layout.createdAt
                )
            )
        }

        // Sort by creation date (newest first)
        items.sortByDescending { it.createdAt }
        _contentItems.value = items
    }

    fun refreshContent() {
        checkCurrentSchedule()
        combineContentItems()
    }

    private fun checkCurrentSchedule() {
        viewModelScope.launch {
            val currentSchedule = getCurrentActiveSchedule()
            _currentSchedule.value = currentSchedule
        }
    }

    private suspend fun getCurrentActiveSchedule(): Schedule? {
        val allSchedules = scheduleRepository.getAllSchedules()
        val currentTime = Calendar.getInstance()
        val currentDayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
        val currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentTime.time)

        return allSchedules.find { schedule ->
            if (!schedule.isActive) return@find false

            // Check if current day is in schedule
            val scheduleDays = schedule.daysOfWeek.split(",").map { it.toIntOrNull() ?: 0 }
            val isCurrentDay = scheduleDays.contains(currentDayOfWeek)

            if (!isCurrentDay) return@find false

            // Check if current time is within schedule time range
            isTimeInRange(currentTimeString, schedule.startTime, schedule.endTime)
        }
    }

    private fun isTimeInRange(currentTime: String, startTime: String, endTime: String): Boolean {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        return try {
            val current = timeFormat.parse(currentTime)?.time ?: 0
            val start = timeFormat.parse(startTime)?.time ?: 0
            val end = timeFormat.parse(endTime)?.time ?: 0

            if (start <= end) {
                // Same day range
                current in start..end
            } else {
                // Overnight range (e.g., 22:00 to 06:00)
                current >= start || current <= end
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getScheduledContent(): PlayerContentItem? {
        val schedule = getCurrentActiveSchedule() ?: return null
        
        return when (schedule.contentType) {
            ContentType.PLAYLIST -> {
                val playlist = playlistRepository.getPlaylistById(schedule.contentId)
                playlist?.let {
                    PlayerContentItem(
                        id = it.id,
                        name = it.name,
                        description = it.description ?: "Scheduled playlist",
                        type = PlayerContentType.PLAYLIST,
                        duration = it.totalDuration,
                        createdAt = it.createdAt
                    )
                }
            }
            ContentType.LAYOUT -> {
                val layout = layoutRepository.getLayoutById(schedule.contentId)
                layout?.let {
                    PlayerContentItem(
                        id = it.id,
                        name = it.name,
                        description = it.description ?: "Scheduled layout",
                        type = PlayerContentType.LAYOUT,
                        duration = 0,
                        createdAt = it.createdAt
                    )
                }
            }
        }
    }
}

data class PlayerContentItem(
    val id: Long,
    val name: String,
    val description: String,
    val type: PlayerContentType,
    val duration: Long,
    val createdAt: Long
)

enum class PlayerContentType {
    PLAYLIST, LAYOUT
}