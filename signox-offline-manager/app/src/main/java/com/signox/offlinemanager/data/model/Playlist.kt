package com.signox.offlinemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val totalDuration: Long = 0, // in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaId", "position"]
)
data class PlaylistItem(
    val playlistId: Long,
    val mediaId: Long,
    val position: Int,
    val duration: Long, // custom duration for this item
    val createdAt: Long = System.currentTimeMillis()
)