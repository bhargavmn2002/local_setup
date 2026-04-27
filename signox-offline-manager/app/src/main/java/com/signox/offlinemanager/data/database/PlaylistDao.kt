package com.signox.offlinemanager.data.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.signox.offlinemanager.data.model.Playlist
import com.signox.offlinemanager.data.model.PlaylistItem

@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): LiveData<List<Playlist>>
    
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsSync(): List<Playlist>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?
    
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistItems(playlistId: Long): LiveData<List<PlaylistItem>>
    
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistItemsSync(playlistId: Long): List<PlaylistItem>
    
    @Query("""
        SELECT m.* FROM media m 
        INNER JOIN playlist_items pi ON m.id = pi.mediaId 
        WHERE pi.playlistId = :playlistId 
        ORDER BY pi.position ASC
    """)
    fun getPlaylistMedia(playlistId: Long): LiveData<List<com.signox.offlinemanager.data.model.Media>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(playlistItem: PlaylistItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(playlistItems: List<PlaylistItem>)
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Update
    suspend fun updatePlaylistItem(playlistItem: PlaylistItem)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    
    @Delete
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem)
    
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItems(playlistId: Long)
    
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeMediaFromPlaylist(playlistId: Long, mediaId: Long)
    
    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
}