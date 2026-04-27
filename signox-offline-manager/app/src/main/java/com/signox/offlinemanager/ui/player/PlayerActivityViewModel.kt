package com.signox.offlinemanager.ui.player

import androidx.lifecycle.ViewModel
import com.signox.offlinemanager.data.model.Layout
import com.signox.offlinemanager.data.model.LayoutZone
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.repository.LayoutRepository
import com.signox.offlinemanager.data.repository.MediaRepository
import com.signox.offlinemanager.data.repository.PlaylistRepository
import com.signox.offlinemanager.data.repository.ScheduleRepository

class PlayerActivityViewModel(
    private val playlistRepository: PlaylistRepository,
    private val layoutRepository: LayoutRepository,
    private val mediaRepository: MediaRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    suspend fun getPlaylistWithMedia(playlistId: Long): Pair<Playlist, List<Media>>? {
        val playlist = playlistRepository.getPlaylistById(playlistId) ?: return null
        val playlistItems = playlistRepository.getPlaylistItems(playlistId)
        
        // Get media items in the correct order with custom durations
        val mediaItems = playlistItems.sortedBy { it.position }.mapNotNull { item ->
            val media = mediaRepository.getMediaById(item.mediaId)
            media?.copy(duration = item.duration) // Use custom duration from playlist item
        }
        
        return Pair(playlist, mediaItems)
    }

    suspend fun getLayoutWithZones(layoutId: Long): Pair<Layout, List<LayoutZoneWithMedia>>? {
        val layout = layoutRepository.getLayoutById(layoutId) ?: return null
        val zones = layoutRepository.getLayoutZones(layoutId)
        
        // Get zones with their associated media/playlists
        val zonesWithMedia = zones.map { zone ->
            val media = zone.mediaId?.let { mediaRepository.getMediaById(it) }
            val playlist = zone.playlistId?.let { playlistRepository.getPlaylistById(it) }
            val playlistMedia = if (playlist != null) {
                playlistRepository.getPlaylistItems(playlist.id).sortedBy { it.position }.mapNotNull { item ->
                    val mediaItem = mediaRepository.getMediaById(item.mediaId)
                    mediaItem?.copy(duration = item.duration)
                }
            } else {
                emptyList()
            }
            
            LayoutZoneWithMedia(zone, media, playlist, playlistMedia)
        }
        
        return Pair(layout, zonesWithMedia)
    }

    suspend fun getScheduledContent(): PlayerContentItem? {
        // Reuse the logic from PlayerViewModel
        val playerViewModel = PlayerViewModel(playlistRepository, layoutRepository, scheduleRepository)
        return playerViewModel.getScheduledContent()
    }
}

data class LayoutZoneWithMedia(
    val zone: LayoutZone,
    val media: Media?,
    val playlist: Playlist?,
    val playlistMedia: List<Media>
)