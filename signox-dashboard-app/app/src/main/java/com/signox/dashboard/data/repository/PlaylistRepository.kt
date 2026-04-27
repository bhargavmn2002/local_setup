package com.signox.dashboard.data.repository

import com.signox.dashboard.data.api.ApiService
import com.signox.dashboard.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    suspend fun getPlaylists(): Result<List<Playlist>> {
        return try {
            val response = apiService.getPlaylists()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.playlists)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch playlists"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlaylist(id: String): Result<Playlist> {
        return try {
            val response = apiService.getPlaylist(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.playlist)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch playlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createPlaylist(name: String): Result<Playlist> {
        return try {
            val response = apiService.createPlaylist(CreatePlaylistRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.playlist)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to create playlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePlaylist(
        id: String,
        name: String,
        items: List<PlaylistItemRequest>
    ): Result<Playlist> {
        return try {
            val response = apiService.updatePlaylist(
                id,
                UpdatePlaylistRequest(name, items)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.playlist)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to update playlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePlaylist(id: String): Result<String> {
        return try {
            val response = apiService.deletePlaylist(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to delete playlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
