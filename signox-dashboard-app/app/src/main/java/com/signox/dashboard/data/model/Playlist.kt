package com.signox.dashboard.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: String,
    val name: String,
    val createdById: String?,
    val createdAt: String,
    val updatedAt: String,
    val items: List<PlaylistItem>?,
    @SerializedName("_count") val count: PlaylistCount?,
    val createdBy: PlaylistUser?
) : Parcelable {
    val itemCount: Int
        get() = count?.items ?: items?.size ?: 0
    
    val totalDuration: Int
        get() = items?.sumOf { it.effectiveDuration } ?: 0
    
    val formattedDuration: String
        get() {
            val seconds = totalDuration
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> String.format("%dh %dm", hours, minutes % 60)
                minutes > 0 -> String.format("%dm %ds", minutes, seconds % 60)
                else -> String.format("%ds", seconds)
            }
        }
    
    val firstMediaUrl: String?
        get() = items?.firstOrNull()?.media?.url
}

@Parcelize
data class PlaylistItem(
    val id: String,
    val playlistId: String,
    val mediaId: String,
    val duration: Int?,
    val order: Int,
    val loopVideo: Boolean?,
    val orientation: String?,
    val resizeMode: String?,
    val rotation: Int?,
    val media: Media
) : Parcelable {
    val effectiveDuration: Int
        get() = duration ?: media.duration ?: 10
    
    val formattedDuration: String
        get() {
            val seconds = effectiveDuration
            val minutes = seconds / 60
            return if (minutes > 0) {
                String.format("%d:%02d", minutes, seconds % 60)
            } else {
                String.format("%ds", seconds)
            }
        }
}

@Parcelize
data class PlaylistCount(
    val items: Int
) : Parcelable

@Parcelize
data class PlaylistUser(
    val id: String,
    val email: String,
    val role: String
) : Parcelable

data class PlaylistsResponse(
    val playlists: List<Playlist>
)

data class PlaylistResponse(
    val playlist: Playlist
)

data class CreatePlaylistRequest(
    val name: String
)

data class UpdatePlaylistRequest(
    val name: String,
    val items: List<PlaylistItemRequest>
)

data class PlaylistItemRequest(
    val mediaId: String,
    val duration: Int?,
    val order: Int,
    val loopVideo: Boolean? = false,
    val orientation: String? = null,
    val resizeMode: String? = "FIT",
    val rotation: Int? = 0
)

data class DeletePlaylistResponse(
    val message: String
)
