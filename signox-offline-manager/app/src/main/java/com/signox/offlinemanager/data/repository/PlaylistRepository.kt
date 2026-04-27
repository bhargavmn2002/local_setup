package com.signox.offlinemanager.data.repository

import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.database.PlaylistDao
import com.signox.offlinemanager.data.model.Media
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.model.PlaylistItem

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun getAllPlaylistsLiveData(): LiveData<List<Playlist>> {
        return playlistDao.getAllPlaylists()
    }

    suspend fun getAllPlaylists(): List<Playlist> {
        return playlistDao.getAllPlaylistsSync()
    }

    suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getPlaylistById(id)
    }

    fun getPlaylistItemsLiveData(playlistId: Long): LiveData<List<PlaylistItem>> {
        return playlistDao.getPlaylistItems(playlistId)
    }

    suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem> {
        return playlistDao.getPlaylistItemsSync(playlistId)
    }

    fun getPlaylistMedia(playlistId: Long): LiveData<List<Media>> {
        return playlistDao.getPlaylistMedia(playlistId)
    }

    suspend fun insertPlaylist(playlist: Playlist): Long {
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun insertPlaylistItem(playlistItem: PlaylistItem) {
        playlistDao.insertPlaylistItem(playlistItem)
    }

    suspend fun insertPlaylistItems(playlistItems: List<PlaylistItem>) {
        playlistDao.insertPlaylistItems(playlistItems)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun updatePlaylistItem(playlistItem: PlaylistItem) {
        playlistDao.updatePlaylistItem(playlistItem)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        // First delete all playlist items
        playlistDao.deletePlaylistItems(playlistId)
        // Then delete the playlist
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun deletePlaylistItem(playlistItem: PlaylistItem) {
        playlistDao.deletePlaylistItem(playlistItem)
    }

    suspend fun removeMediaFromPlaylist(playlistId: Long, mediaId: Long) {
        playlistDao.removeMediaFromPlaylist(playlistId, mediaId)
    }

    suspend fun getPlaylistCount(): Int {
        return playlistDao.getPlaylistCount()
    }
}